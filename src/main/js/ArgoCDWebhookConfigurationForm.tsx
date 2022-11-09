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
