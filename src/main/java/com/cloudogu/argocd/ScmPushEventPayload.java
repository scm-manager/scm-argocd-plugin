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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sonia.scm.repository.Branch;

import javax.xml.bind.annotation.XmlElement;


/*
 * We use the GitHub Push Event instead of implementing our own webhook event definition into ArgoCD.
 * We do not send information like the changed files or revisions on purpose.
 * ArgoCD assumes if nothing has been sent that it must refresh all related cluster resources for that repository which is exactly what we want.
 */
@Getter
public class ScmPushEventPayload {
  @XmlElement(name = "html_url")
  private String htmlUrl;
  @XmlElement(name = "branch")
  private WebhookBranch branch;

  public ScmPushEventPayload(String htmlUrl, boolean defaultBranch, String branchName) {
    this.htmlUrl = htmlUrl;
    this.branch = new WebhookBranch(defaultBranch, branchName);
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Getter
  public class WebhookBranch{
    @XmlElement(name = "default_branch")
    private boolean defaultBranch;
    @XmlElement(name = "name")
    private String name;
  }
}

