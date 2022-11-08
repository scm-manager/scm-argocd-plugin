package com.cloudogu.argocd;

import sonia.scm.plugin.ExtensionPoint;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.Repository;
import sonia.scm.webhook.WebHookExecutor;
import sonia.scm.webhook.WebHookExecutorProvider;
import sonia.scm.webhook.WebHookHttpClient;

import javax.inject.Inject;
import javax.inject.Provider;

@ExtensionPoint
public class ArgoCDWebhookExecutorProvider implements WebHookExecutorProvider<ArgoCDWebhook> {

  private final Provider<WebHookHttpClient> clientProvider;

  @Inject
  public ArgoCDWebhookExecutorProvider(Provider<WebHookHttpClient> clientProvider) {
    this.clientProvider = clientProvider;
  }

  @Override
  public boolean handles(Class aClass) {
    return ArgoCDWebhook.class.isAssignableFrom(aClass);
  }

  @Override
  public WebHookExecutor createExecutor(ArgoCDWebhook webHook, Repository repository, Iterable<Changeset> changesets) {
    return new ArgoCDWebhookExecutor(clientProvider.get(), webHook, repository);
  }
}
