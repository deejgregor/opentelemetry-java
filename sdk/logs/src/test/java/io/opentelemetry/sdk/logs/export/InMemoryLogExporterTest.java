/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.logs.export;

import static io.opentelemetry.sdk.logs.data.Severity.DEBUG;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.logs.LogEmitter;
import io.opentelemetry.sdk.logs.SdkLogEmitterProvider;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.testing.assertj.LogAssertions;
import io.opentelemetry.sdk.testing.logs.TestLogData;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link InMemoryLogExporter}. */
class InMemoryLogExporterTest {
  private final InMemoryLogExporter exporter = InMemoryLogExporter.create();

  private SdkLogEmitterProvider logEmitterProvider;
  private LogEmitter logEmitter;

  @BeforeEach
  void setup() {
    logEmitterProvider =
        SdkLogEmitterProvider.builder()
            .addLogProcessor(SimpleLogProcessor.create(exporter))
            .build();
    logEmitter = logEmitterProvider.logEmitterBuilder("emitter").build();
  }

  @AfterEach
  void tearDown() {
    logEmitterProvider.shutdown();
  }

  @Test
  void getFinishedLogItems() {
    logEmitter.logBuilder().setSeverity(DEBUG).setBody("message 1").emit();
    logEmitter.logBuilder().setSeverity(DEBUG).setBody("message 2").emit();
    logEmitter.logBuilder().setSeverity(DEBUG).setBody("message 3").emit();

    List<LogData> logItems = exporter.getFinishedLogItems();
    assertThat(logItems).isNotNull();
    assertThat(logItems.size()).isEqualTo(3);
    LogAssertions.assertThat(logItems.get(0)).hasBody("message 1");
    LogAssertions.assertThat(logItems.get(1)).hasBody("message 2");
    LogAssertions.assertThat(logItems.get(2)).hasBody("message 3");
  }

  @Test
  void reset() {
    logEmitter.logBuilder().setSeverity(DEBUG).setBody("message 1").emit();
    logEmitter.logBuilder().setSeverity(DEBUG).setBody("message 2").emit();
    logEmitter.logBuilder().setSeverity(DEBUG).setBody("message 3").emit();
    List<LogData> logItems = exporter.getFinishedLogItems();
    assertThat(logItems).isNotNull();
    assertThat(logItems.size()).isEqualTo(3);
    // Reset then expect no items in memory.
    exporter.reset();
    assertThat(exporter.getFinishedLogItems()).isEmpty();
  }

  @Test
  void shutdown() {
    logEmitter.logBuilder().setSeverity(DEBUG).setBody("message 1").emit();
    logEmitter.logBuilder().setSeverity(DEBUG).setBody("message 2").emit();
    logEmitter.logBuilder().setSeverity(DEBUG).setBody("message 3").emit();
    List<LogData> logItems = exporter.getFinishedLogItems();
    assertThat(logItems).isNotNull();
    assertThat(logItems.size()).isEqualTo(3);
    // Shutdown then expect no items in memory.
    exporter.shutdown();
    assertThat(exporter.getFinishedLogItems()).isEmpty();
    // Cannot add new elements after the shutdown.
    logEmitter.logBuilder().setSeverity(DEBUG).setBody("message 1").emit();
    assertThat(exporter.getFinishedLogItems()).isEmpty();
  }

  @Test
  void export_ReturnCode() {
    LogData logData = TestLogData.builder().setBody("message 1").setSeverity(DEBUG).build();
    assertThat(exporter.export(Collections.singletonList(logData)).isSuccess()).isTrue();
    exporter.shutdown();
    // After shutdown no more export.
    assertThat(exporter.export(Collections.singletonList(logData)).isSuccess()).isFalse();
    exporter.reset();
    // Reset does not do anything if already shutdown.
    assertThat(exporter.export(Collections.singletonList(logData)).isSuccess()).isFalse();
  }
}
