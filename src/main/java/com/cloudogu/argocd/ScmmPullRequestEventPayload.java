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


@Getter
@EqualsAndHashCode
@ToString
class ScmmPullRequestEventPayload implements PullRequestEventPayload {
  @XmlElement(name = "repository")
  private final Repository repository;
  @XmlElement(name = "sourceBranch")
  private final WebhookBranch sourceBranch;
  @XmlElement(name = "targetBranch")
  private final WebhookBranch targetBranch;
  @XmlElement(name = "action")
  private final String action;

  ScmmPullRequestEventPayload(Repository repository, String sourceBranchName, String targetBranchName, Action action) {
    this.repository = repository;
    this.sourceBranch = new WebhookBranch(sourceBranchName);
    this.targetBranch = new WebhookBranch(targetBranchName);
    this.action = action.name().toLowerCase();
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @EqualsAndHashCode
  @Getter
  @ToString
  static class Repository {
    @XmlElement(name = "sourceUrl")
    private String sourceUrl;
    @XmlElement(name = "namespace")
    private String namespace;
    @XmlElement(name = "name")
    private String name;
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @EqualsAndHashCode
  @Getter
  @ToString
  static class WebhookBranch {
    @XmlElement(name = "name")
    private String name;
  }

  enum Action {
    OPENED, MERGED, REJECTED, CHANGED
  }
}

