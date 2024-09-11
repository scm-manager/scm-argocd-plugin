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

import { binder} from "@scm-manager/ui-extensions";
import ArgoCDWebhookConfigurationForm, { ArgoCDWebhook } from "./ArgoCDWebhookConfigurationForm";
import { WebhookConfiguration } from "@scm-manager/scm-webhook-plugin";
import ArgoCDOverviewCardTop from "./ArgoCDOverviewCardTop";
import ArgoCDOverviewCardBottom from "./ArgoCDOverviewCardBottom";

binder.bind<WebhookConfiguration<ArgoCDWebhook>>("webhook.configuration", {
  name: "ArgoCDWebhook",
  FormComponent: ArgoCDWebhookConfigurationForm,
  OverviewCardTop: ArgoCDOverviewCardTop,
  OverviewCardBottom: ArgoCDOverviewCardBottom,
  defaultConfiguration: {
    url: "",
    secret: "",
    insecure: false
  }
});

