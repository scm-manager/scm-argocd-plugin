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
import { Form } from "@scm-manager/ui-forms";
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
      <div className="content">
        <ul>
          <li><b>{t("scm-argocd-plugin.config.hookImplementation")}</b>&nbsp;&ndash;&nbsp;{t("scm-argocd-plugin.config.hookImplementationHelpText")}</li>
          <li><b>{t("scm-argocd-plugin.config.url")}</b>&nbsp;&ndash;&nbsp;{t("scm-argocd-plugin.config.urlHelpText")}</li>
          <li><b>{t("scm-argocd-plugin.config.secret")}</b>&nbsp;&ndash;&nbsp;{t("scm-argocd-plugin.config.secretHelpText")}</li>
          <li><b>{t("scm-argocd-plugin.config.insecure")}</b>&nbsp;&ndash;&nbsp;{t("scm-argocd-plugin.config.insecureHelpText")}</li>
        </ul>
      </div>
      <Form.Row>
        <Form.Select
          className="column"
          name="hookImplementation"
          value={webhook.hookImplementation}
          label={t("scm-argocd-plugin.config.hookImplementation")}
          defaultValue="SCMM"
          options={[{ label: t("scm-argocd-plugin.config.hookImplementationTypes.SCMM"), value: "SCMM" }, { label: t("scm-argocd-plugin.config.hookImplementationTypes.GITHUB"), value: "GITHUB" }]}
        />
      </Form.Row>
      <Form.Row>
        <Form.Input
          name="url"
          label={t("scm-argocd-plugin.config.url")}
        />
      </Form.Row>
      <Form.Row>
        <Form.Input
          name="secret"
          label={t("scm-argocd-plugin.config.secret")}
          type="password"
        />
      </Form.Row>
      <Form.Row>
        <Form.Checkbox
          name="insecure"
          label={t("scm-argocd-plugin.config.insecure")}
        />
      </Form.Row>
      {webhook.insecure ? (
        <Notification type="warning">{t("scm-argocd-plugin.config.insecureWarning")}</Notification>
      ) : null}
    </>
  );
};

export default ArgoCDWebhookConfigurationForm;
