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

import lombok.AllArgsConstructor;
import lombok.Getter;

import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;


/*
 * We use the GitHub Push Event instead of implementing our own webhook event definition into ArgoCD.
 * We do not send information like the changed files or revisions on purpose.
 * ArgoCD assumes if nothing has been sent that it must refresh all related cluster resources for that repository which is exactly what we want.
 */
@Getter
public class GitHubPushEventPayloadDto implements PushEventPayload {
  private final GitHubRepository repository;
  private final List<String> commits;
  private final String ref;

  public GitHubPushEventPayloadDto(GitHubRepository repository, String branch) {
    this.repository = repository;
    this.ref =  "refs/heads/" + branch;
    this.commits = new ArrayList<>();
  }
}

@AllArgsConstructor
@Getter
class GitHubRepository {
  @XmlElement(name = "html_url")
  private String htmlUrl;
  @XmlElement(name = "default_branch")
  private String defaultBranch;
}

