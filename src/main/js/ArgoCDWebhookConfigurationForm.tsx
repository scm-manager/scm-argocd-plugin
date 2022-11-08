import { InputField } from "@scm-manager/ui-components";
import React from "react";
import { FC, useState } from "react";
import { ArgoCDWebhook } from "./ArgoCDWebhook";

type Props = {
  webHook: ArgoCDWebhook;
  readOnly: boolean;
  onChange: (p: ArgoCDWebhook) => void;
  onDelete: (p: ArgoCDWebhook) => void;
};

const ArgoCDWebhookConfigurationForm: FC<Props> = ({ webHook, readOnly, onChange, onDelete }) => {
  const [webhookState, setWebhookState] = useState<ArgoCDWebhook>(webHook);

  return (
    <>
      <InputField
        label="URL"
        value={webhookState.url}
        onChange={value => setWebhookState({ ...webhookState, url: value })}
      />
      <InputField
        label="PAYLOAD"
        value={webhookState.payload}
        onChange={value => setWebhookState({ ...webhookState, payload: value })}
      />
    </>
  );
};

export default ArgoCDWebhookConfigurationForm
