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

import React, { FC } from "react";
import { useTranslation } from "react-i18next";
import { Form } from "@scm-manager/ui-forms";
import { Notification } from "@scm-manager/ui-components";
import { SelectField } from "@scm-manager/ui-forms";

export type ArgoCDWebhook = {
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
