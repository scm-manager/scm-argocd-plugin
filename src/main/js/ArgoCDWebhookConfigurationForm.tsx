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

import { InputField } from "@scm-manager/ui-components";
import React, { FC, useEffect, useState } from "react";
import { ArgoCDWebhook } from "./ArgoCDWebhook";
import { useTranslation } from "react-i18next";

type Props = {
  webHook: any;
  readOnly: boolean;
  onChange: (p: ArgoCDWebhook, valid: boolean) => void;
};

const ArgoCDWebhookConfigurationForm: FC<Props> = ({ webHook, readOnly, onChange }) => {
  const [webhookState, setWebhookState] = useState<ArgoCDWebhook>(webHook.configuration);
  const [t] = useTranslation("plugins");

  useEffect(() => {
    console.log(webHook, webhookState)
    onChange(webhookState, isConfigValid());
  }, [webhookState]);

  const isConfigValid = () => !!webhookState.payload && !!webhookState.url;

  return (
    <>
      <InputField
        label={t("scm-argocd-plugin.config.url")}
        value={webhookState.url}
        onChange={value => setWebhookState({ ...webhookState, url: value })}
        readOnly={readOnly}
      />
      <InputField
        label={t("scm-argocd-plugin.config.payload")}
        value={webhookState.payload}
        onChange={value => setWebhookState({ ...webhookState, payload: value })}
        readOnly={readOnly}
      />
    </>
  );
};

export default ArgoCDWebhookConfigurationForm;