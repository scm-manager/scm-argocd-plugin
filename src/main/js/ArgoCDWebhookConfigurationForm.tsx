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

import React, { FC } from "react";
import { useTranslation } from "react-i18next";
import { Form, SelectField } from "@scm-manager/ui-forms";
import { Notification } from "@scm-manager/ui-components";

export type ArgoCDWebhook = {
  hookImplementation: string;
  url: string;
  secret: string;
  insecure: boolean;
};

type Props = {
  webhook: ArgoCDWebhook;
};

const ArgoCDWebhookConfigurationForm: FC<Props> = ({ webhook }) => {
  const [t] = useTranslation("plugins");

  return (
    <>
      <Form.Row>
        <Form.Select
          className="column"
          name="hookImplementation"
          // value={webhook.hookImplementation}
          label={t("scm-argocd-plugin.config.hookImplementation")}
          helpText={t("scm-argocd-plugin.config.hookImplementationHelpText")}
          defaultValue="SCMM"
          options={[{ label: t("scm-argocd-plugin.config.hookImplementationTypes.SCMM"), value: "SCMM" }, { label: t("scm-argocd-plugin.config.hookImplementationTypes.GITHUB"), value: "GITHUB" }]}
        />
      </Form.Row>
      <Form.Row>
        <SelectField
          className="column"
          label={t("scm-argocd-plugin.config.httpMethod")}
          defaultValue="POST"
          options={[{ label: "POST", value: "POST" }]}
          disabled
        />
      </Form.Row>
      <Form.Row>
        <Form.Input
          name="url"
          label={t("scm-argocd-plugin.config.url")}
          helpText={t("scm-argocd-plugin.config.urlHelpText")}
        />
      </Form.Row>
      <Form.Row>
        <Form.Input
          name="secret"
          label={t("scm-argocd-plugin.config.secret")}
          helpText={t("scm-argocd-plugin.config.secretHelpText")}
          type="password"
        />
      </Form.Row>
      <Form.Row>
        <Form.Checkbox
          name="insecure"
          label={t("scm-argocd-plugin.config.insecure")}
          helpText={t("scm-argocd-plugin.config.insecureHelpText")}
        />
      </Form.Row>
      {webhook.insecure ? (
        <Notification type="warning">{t("scm-argocd-plugin.config.insecureWarning")}</Notification>
      ) : null}
    </>
  );
};

export default ArgoCDWebhookConfigurationForm;
