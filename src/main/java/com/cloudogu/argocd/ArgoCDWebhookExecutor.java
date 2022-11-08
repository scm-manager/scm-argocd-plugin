package com.cloudogu.argocd;

import sonia.scm.ContextEntry;
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
