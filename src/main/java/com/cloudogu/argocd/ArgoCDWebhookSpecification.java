/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package com.cloudogu.argocd;

import com.google.inject.Provider;
import jakarta.inject.Inject;
import org.apache.commons.lang.StringUtils;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.webhook.WebHookExecutor;
import sonia.scm.webhook.WebHookService;
import sonia.scm.webhook.WebHookSpecification;

@Extension
public class ArgoCDWebhookSpecification implements WebHookSpecification<ArgoCDWebhook> {

  public static final String DUMMY_SECRET = "__DUMMY__";
  private final Provider<WebHookService> webHookService;
  private final RepositoryServiceFactory serviceFactory;

  @Inject
  public ArgoCDWebhookSpecification(Provider<WebHookService> webHookService, RepositoryServiceFactory serviceFactory) {
    this.webHookService = webHookService;
    this.serviceFactory = serviceFactory;
  }

  @Override
  public Class<ArgoCDWebhook> getSpecificationType() {
    return ArgoCDWebhook.class;
  }

  @Override
  public boolean supportsRepository(Repository repository) {
    return "git".equals(repository.getType());
  }

  @Override
  public WebHookExecutor createExecutor(ArgoCDWebhook webHook, Repository repository, PostReceiveRepositoryHookEvent event) {
    return new ArgoCDCommitExecutor(new ArgoCDSender(webHookService.get()), serviceFactory, webHook, repository, event);
  }

  @Override
  public ArgoCDWebhook mapToDto(ArgoCDWebhook configuration) {
    if (StringUtils.isEmpty(configuration.getSecret())) {
      return configuration;
    } else {
      return new ArgoCDWebhook(configuration.getHookImplementation(), configuration.getUrl(), DUMMY_SECRET, configuration.isInsecure());
    }
  }

  @Override
  public void updateBeforeStore(ArgoCDWebhook oldConfiguration, ArgoCDWebhook newConfiguration) {
    if (DUMMY_SECRET.equals(newConfiguration.getSecret())) {
      newConfiguration.setSecret(oldConfiguration.getSecret());
    }
  }
}
