/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.cloudogu.argocd;

import com.google.common.base.Strings;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import sonia.scm.net.ahc.AdvancedHttpClient;
import sonia.scm.net.ahc.AdvancedHttpRequestWithBody;
import sonia.scm.repository.Branch;
import sonia.scm.repository.InternalRepositoryException;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.HookBranchProvider;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.repository.api.ScmProtocol;
import sonia.scm.webhook.WebHookExecutor;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ArgoCDWebhookExecutor implements WebHookExecutor {

  private final AdvancedHttpClient client;

  private final RepositoryServiceFactory serviceFactory;
  private final ArgoCDWebhook webhook;
  private final Repository repository;
  private final PostReceiveRepositoryHookEvent event;

  public ArgoCDWebhookExecutor(AdvancedHttpClient client,
                               RepositoryServiceFactory serviceFactory,
                               ArgoCDWebhook webhook,
                               Repository repository,
                               PostReceiveRepositoryHookEvent event) {
    this.client = client;
    this.serviceFactory = serviceFactory;
    this.webhook = webhook;
    this.repository = repository;
    this.event = event;
  }

  @Override
  public void run() {
    HookBranchProvider branchProvider = event.getContext().getBranchProvider();
    try (RepositoryService service = serviceFactory.create(repository)) {
      String defaultBranch = findDefaultBranch(service);
      String htmlUrl = findHtmlUrl(service);
      branchProvider.getCreatedOrModified().forEach(branch -> sendEvent(new ScmPushEventPayload(htmlUrl, defaultBranch.equals(branch), branch)));
      branchProvider.getDeletedOrClosed().forEach(branch -> sendEvent(new ScmPushEventPayload(htmlUrl, defaultBranch.equals(branch), branch)));
  } catch (IOException e) {
      throw new InternalRepositoryException(repository, "Failed to trigger ArgoCD Webhook", e);
    }
  }

  private void sendEvent(ScmPushEventPayload payload) {
    try {
      AdvancedHttpRequestWithBody request = client.post(webhook.getUrl())
        .header("X-SCM-PushEvent", "Push")
        .spanKind("Webhook")
        .contentType(MediaType.APPLICATION_JSON)
        .jsonContent(payload);

      if (webhook.isInsecure()) {
        // We introduced this flag for testing environments
        request
          .disableCertificateValidation(true)
          .disableHostnameValidation(true);
      }

      if (!Strings.isNullOrEmpty(webhook.getSecret())) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
          request.getContent().process(baos);
          String digest = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, webhook.getSecret()).hmacHex(baos.toByteArray());
          request.header("X-SCM-Signature", "sha1=" + digest);
        }
      }

      request
        .request();

    } catch (IOException e) {
      throw new ArgoCDHookExecutionException(
        "Could not execute ArgoCD webhook",
        e
      );
    }
  }

  private String findHtmlUrl(RepositoryService service) {
    return service.getSupportedProtocols()
      .filter(p -> "http".equals(p.getType()))
      .findFirst()
      .map(ScmProtocol::getUrl)
      .orElseThrow(() -> new ArgoCDHookExecutionException("Http protocol not found for repository " + repository));
  }

  private String findDefaultBranch(RepositoryService service) throws IOException {
    return service.getBranchesCommand().getBranches().getBranches().stream()
      .filter(Branch::isDefaultBranch)
      .findFirst().map(Branch::getName)
      .orElseThrow(() -> new InternalRepositoryException(repository, "Could not find default branch"));
  }
}
