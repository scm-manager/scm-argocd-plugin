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

import sonia.scm.repository.Repository;
import sonia.scm.webhook.HttpMethod;
import sonia.scm.webhook.WebHookExecutor;
import sonia.scm.webhook.WebHookHttpClient;

import javax.inject.Inject;
import java.io.IOException;

import static sonia.scm.ContextEntry.ContextBuilder.entity;

public class ArgoCDWebhookExecutor implements WebHookExecutor {

  private final WebHookHttpClient client;
  private final ArgoCDWebhook webhook;
  private final Repository repository;

  @Inject
  public ArgoCDWebhookExecutor(WebHookHttpClient client, ArgoCDWebhook webhook, Repository repository) {
    this.client = client;
    this.webhook = webhook;
    this.repository = repository;
  }

  @Override
  public void run() {
    try {
      // TODO: ??? This way we get the generic span kind "Webhook" in the trace monitor
      client.execute(HttpMethod.POST, webhook.getUrl(), webhook.getPayload());
    } catch (IOException e) {
      throw new ArgoCDHookExecutionException(
        entity(Repository.class, repository.getNamespaceAndName().toString()).build(),
        "Could not execute ArgoCD webhook",
        e
      );
    }
  }
}
