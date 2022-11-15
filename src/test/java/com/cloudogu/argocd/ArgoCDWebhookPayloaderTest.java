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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Branch;
import sonia.scm.repository.Branches;
import sonia.scm.repository.Person;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.api.BranchesCommandBuilder;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.repository.api.ScmProtocol;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.list;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArgoCDWebhookPayloaderTest {

  private final Repository repository = RepositoryTestData.create42Puzzle();

  @Mock
  private RepositoryServiceFactory serviceFactory;

  @Mock
  private RepositoryService service;
  @Mock
  private BranchesCommandBuilder branchesCommandBuilder;
  @Mock
  private Stream<ScmProtocol> protocolStream;


  @InjectMocks
  private ArgoCDWebhookPayloader payloader;



  @Test
  void shouldCreatePayload() throws IOException {
    when(serviceFactory.create(repository)).thenReturn(service);
    when(service.getSupportedProtocols()).thenReturn(protocolStream);
    when(service.getBranchesCommand()).thenReturn(branchesCommandBuilder);
    when(branchesCommandBuilder.getBranches()).thenReturn(new Branches(List.of(Branch.defaultBranch("main", "123", 0L, Person.toPerson("")))));
    when(protocolStream.collect(any())).thenReturn(List.of(new ScmProtocol() {
      @Override
      public String getType() {
        return "http";
      }

      @Override
      public String getUrl() {
        return "my-repo-url";
      }
    }));

    GitHubPushEventPayloadDto payload = payloader.createPayload(repository);

    assertThat(payload.getRef()).isEqualTo("refs/heads/main");
    assertThat(payload.getCommits()).isEmpty();
    assertThat(payload.getRepository().getDefaultBranch()).isEqualTo("main");
    assertThat(payload.getRepository().getHtmlUrl()).isEqualTo("my-repo-url");
  }
}
