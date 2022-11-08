package com.cloudogu.argocd;

import sonia.scm.ContextEntry;
import sonia.scm.ExceptionWithContext;

import java.util.List;

public class ArgoCDHookExecutionException extends ExceptionWithContext {

  protected ArgoCDHookExecutionException(List<ContextEntry> context, String message, Exception cause) {
    super(context, message, cause);
  }

  @Override
  public String getCode() {
    return "2ATMbdEev1";
  }
}
