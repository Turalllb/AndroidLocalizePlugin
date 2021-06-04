package com.airsaid.localization.translate;

import com.airsaid.localization.config.SettingsState;
import com.airsaid.localization.translate.lang.Lang;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import com.intellij.util.io.HttpRequests;
import com.intellij.util.io.RequestBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author airsaid
 */
public abstract class AbstractTranslator implements Translator, TranslatorConfigurable {

  protected static final Logger LOG = Logger.getInstance(AbstractTranslator.class);

  private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";

  @Override
  public String doTranslate(@NotNull Lang fromLang, @NotNull Lang toLang, @NotNull String text) throws TranslationException {
    List<Lang> supportedLanguages = getSupportedLanguages();
    if (!supportedLanguages.contains(toLang)) {
      throw new TranslationException(fromLang, toLang, text, toLang.getEnglishName() + " is not supported.");
    }

    String requestUrl = getRequestUrl(fromLang, toLang, text);
    String requestParams = getRequestParams(fromLang, toLang, text)
        .stream()
        .map(pair -> pair.first.concat("=").concat(pair.second))
        .collect(Collectors.joining("&"));

    RequestBuilder requestBuilder = HttpRequests.post(requestUrl, CONTENT_TYPE);
    configureRequestBuilder(requestBuilder);

    try {
      return requestBuilder.connect(request -> {
        request.write(requestParams);
        String resultText = request.readString();
        return parsingResult(fromLang, toLang, text, resultText);
      });
    } catch (IOException e) {
      e.printStackTrace();
      throw new TranslationException(fromLang, toLang, text, e);
    }
  }

  @Override
  public @Nullable Icon getIcon() {
    return null;
  }

  @Override
  public @Nullable String getAppId() {
    return SettingsState.getInstance().getAppId(getKey());
  }

  @Override
  public @Nullable String getAppKey() {
    return SettingsState.getInstance().getAppKey(getKey());
  }

  @Override
  public @Nullable String getApplyAppIdUrl() {
    return null;
  }

  @NotNull
  public abstract String getRequestUrl(@NotNull Lang fromLang, @NotNull Lang toLang, @NotNull String text);

  @NotNull
  public abstract List<Pair<String, String>> getRequestParams(@NotNull Lang fromLang, @NotNull Lang toLang, @NotNull String text);

  public abstract void configureRequestBuilder(@NotNull RequestBuilder requestBuilder);

  @NotNull
  public abstract String parsingResult(@NotNull Lang fromLang, @NotNull Lang toLang, @NotNull String text, @NotNull String resultText);
}