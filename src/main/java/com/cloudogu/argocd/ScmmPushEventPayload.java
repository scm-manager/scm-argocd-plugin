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

import jakarta.xml.bind.annotation.XmlElement;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;


/*
 * We use the GitHub Push Event instead of implementing our own webhook event definition into ArgoCD.
 * We do not send information like the changed files or revisions on purpose.
 * ArgoCD assumes if nothing has been sent that it must refresh all related cluster resources for that repository which is exactly what we want.
 */
@Getter
@ToString
class ScmmPushEventPayload implements PushEventPayload {
  @XmlElement(name = "repository")
  private final Repository repository;
  @XmlElement(name = "branch")
  private final WebhookBranch branch;

  ScmmPushEventPayload(String repository, boolean defaultBranch, String branchName) {
    this.repository = new Repository(repository);
    this.branch = new WebhookBranch(defaultBranch, branchName);
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Getter
  static class Repository {
    @XmlElement(name = "sourceUrl")
    private String sourceUrl;
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Getter
  @EqualsAndHashCode
  @ToString
  static class WebhookBranch {
    @XmlElement(name = "defaultBranch")
    private boolean defaultBranch;
    @XmlElement(name = "name")
    private String name;
  }
}

