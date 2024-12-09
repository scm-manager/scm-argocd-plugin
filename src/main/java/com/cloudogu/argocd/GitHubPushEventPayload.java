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
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;


/*
 * We do not send information like the changed files or revisions on purpose.
 * ArgoCD assumes if nothing has been sent that it must refresh all related cluster resources for that repository which is exactly what we want.
 */
@Getter
@ToString
@EqualsAndHashCode
class GitHubPushEventPayload implements PushEventPayload {
  private final GitHubRepository repository;
  private final List<String> commits;
  private final String ref;

  GitHubPushEventPayload(String sourceUrl, String defaultBranch, String branch) {
    this.repository = new GitHubRepository(sourceUrl, defaultBranch);
    this.ref = "refs/heads/" + branch;
    this.commits = new ArrayList<>();
  }

  @AllArgsConstructor
  @Getter
  @ToString
  @EqualsAndHashCode
  static class GitHubRepository {
    // HTML URL has been chosen deliberately; it is from the GitHub API:
    @XmlElement(name = "html_url")
    private String htmlUrl;
    @XmlElement(name = "default_branch")
    private String defaultBranch;
  }
}

