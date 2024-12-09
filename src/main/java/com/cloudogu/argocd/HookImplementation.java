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

import sonia.scm.webhook.WebhookHeader;

enum HookImplementation {
  SCMM {
    @Override
    WebhookHeader getHeader(EventType eventType) {
      return switch (eventType) {
        case PUSH_EVENT -> new WebhookHeader("X-SCM-Event", "Push", false);
        case PULL_REQUEST_EVENT -> new WebhookHeader("X-SCM-Event", "PullRequest", false);
      };
    }

    @Override
    String getSecurityHeaderKey() {
      return "X-SCM-Signature";
    }
  },
  GITHUB {
    @Override
    WebhookHeader getHeader(EventType eventType) {
      return switch (eventType) {
        case PUSH_EVENT -> new WebhookHeader("X-Github-Event", "Push", false);
        case PULL_REQUEST_EVENT -> new WebhookHeader("X-Github-Event", "PullRequest", false);
      };
    }

    @Override
    String getSecurityHeaderKey() {
      return "X-Hub-Signature";
    }
  };

  abstract WebhookHeader getHeader(EventType eventType);

  abstract String getSecurityHeaderKey();

  enum EventType {PUSH_EVENT, PULL_REQUEST_EVENT}
}
