/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.paimon.flink;

import org.apache.paimon.CoreOptions;
import org.apache.paimon.CoreOptions.StreamingReadMode;
import org.apache.paimon.annotation.Documentation.ExcludeFromDocumentation;
import org.apache.paimon.options.ConfigOption;
import org.apache.paimon.options.ConfigOptions;
import org.apache.paimon.options.MemorySize;
import org.apache.paimon.options.description.DescribedEnum;
import org.apache.paimon.options.description.Description;
import org.apache.paimon.options.description.InlineElement;
import org.apache.paimon.options.description.TextElement;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.apache.paimon.CoreOptions.STREAMING_READ_MODE;
import static org.apache.paimon.options.ConfigOptions.key;
import static org.apache.paimon.options.description.TextElement.text;

/** Options for flink connector. */
public class FlinkConnectorOptions {

    public static final String NONE = "none";

    public static final ConfigOption<String> LOG_SYSTEM =
            ConfigOptions.key("log.system")
                    .stringType()
                    .defaultValue(NONE)
                    .withDescription(
                            Description.builder()
                                    .text("The log system used to keep changes of the table.")
                                    .linebreak()
                                    .linebreak()
                                    .text("Possible values:")
                                    .linebreak()
                                    .list(
                                            TextElement.text(
                                                    "\"none\": No log system, the data is written only to file store,"
                                                            + " and the streaming read will be directly read from the file store."))
                                    .list(
                                            TextElement.text(
                                                    "\"kafka\": Kafka log system, the data is double written to file"
                                                            + " store and kafka, and the streaming read will be read from kafka. If streaming read from file, configures "
                                                            + STREAMING_READ_MODE.key()
                                                            + " to "
                                                            + StreamingReadMode.FILE.getValue()
                                                            + "."))
                                    .build());

    public static final ConfigOption<Integer> LOG_SYSTEM_PARTITIONS =
            ConfigOptions.key("log.system.partitions")
                    .intType()
                    .defaultValue(1)
                    .withDescription(
                            "The number of partitions of the log system. If log system is kafka, this is kafka partitions.");

    public static final ConfigOption<Integer> LOG_SYSTEM_REPLICATION =
            ConfigOptions.key("log.system.replication")
                    .intType()
                    .defaultValue(1)
                    .withDescription(
                            "The number of replication of the log system. If log system is kafka, this is kafka replicationFactor.");

    public static final ConfigOption<Integer> SINK_PARALLELISM =
            ConfigOptions.key("sink.parallelism")
                    .intType()
                    .noDefaultValue()
                    .withDescription(
                            "Defines a custom parallelism for the sink. "
                                    + "By default, if this option is not defined, the planner will derive the parallelism "
                                    + "for each statement individually by also considering the global configuration.");

    public static final ConfigOption<Integer> SCAN_PARALLELISM =
            ConfigOptions.key("scan.parallelism")
                    .intType()
                    .noDefaultValue()
                    .withDescription(
                            "Define a custom parallelism for the scan source. "
                                    + "By default, if this option is not defined, the planner will derive the parallelism "
                                    + "for each statement individually by also considering the global configuration. "
                                    + "If user enable the scan.infer-parallelism, the planner will derive the parallelism by inferred parallelism.");

    public static final ConfigOption<Integer> UNAWARE_BUCKET_COMPACTION_PARALLELISM =
            ConfigOptions.key("unaware-bucket.compaction.parallelism")
                    .intType()
                    .noDefaultValue()
                    .withDescription(
                            "Defines a custom parallelism for the unaware-bucket table compaction job. "
                                    + "By default, if this option is not defined, the planner will derive the parallelism "
                                    + "for each statement individually by also considering the global configuration.");

    public static final ConfigOption<Boolean> INFER_SCAN_PARALLELISM =
            ConfigOptions.key("scan.infer-parallelism")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription(
                            "If it is false, parallelism of source are set by "
                                    + SCAN_PARALLELISM.key()
                                    + ". Otherwise, source parallelism is inferred from splits number (batch mode) or bucket number(streaming mode).");

    @Deprecated
    @ExcludeFromDocumentation("Deprecated")
    public static final ConfigOption<Duration> CHANGELOG_PRODUCER_FULL_COMPACTION_TRIGGER_INTERVAL =
            key("changelog-producer.compaction-interval")
                    .durationType()
                    .defaultValue(Duration.ofSeconds(0))
                    .withDescription(
                            "When "
                                    + CoreOptions.CHANGELOG_PRODUCER.key()
                                    + " is set to "
                                    + CoreOptions.ChangelogProducer.FULL_COMPACTION.name()
                                    + ", full compaction will be constantly triggered after this interval.");

    public static final ConfigOption<Boolean> CHANGELOG_PRODUCER_LOOKUP_WAIT =
            key("changelog-producer.lookup-wait")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription(
                            "When "
                                    + CoreOptions.CHANGELOG_PRODUCER.key()
                                    + " is set to "
                                    + CoreOptions.ChangelogProducer.LOOKUP.name()
                                    + ", commit will wait for changelog generation by lookup.");

    public static final ConfigOption<WatermarkEmitStrategy> SCAN_WATERMARK_EMIT_STRATEGY =
            key("scan.watermark.emit.strategy")
                    .enumType(WatermarkEmitStrategy.class)
                    .defaultValue(WatermarkEmitStrategy.ON_EVENT)
                    .withDescription("Emit strategy for watermark generation.");

    public static final ConfigOption<String> SCAN_WATERMARK_ALIGNMENT_GROUP =
            key("scan.watermark.alignment.group")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("A group of sources to align watermarks.");

    public static final ConfigOption<Duration> SCAN_WATERMARK_IDLE_TIMEOUT =
            key("scan.watermark.idle-timeout")
                    .durationType()
                    .noDefaultValue()
                    .withDescription(
                            "If no records flow in a partition of a stream for that amount of time, then"
                                    + " that partition is considered \"idle\" and will not hold back the progress of"
                                    + " watermarks in downstream operators.");

    public static final ConfigOption<Duration> SCAN_WATERMARK_ALIGNMENT_MAX_DRIFT =
            key("scan.watermark.alignment.max-drift")
                    .durationType()
                    .noDefaultValue()
                    .withDescription(
                            "Maximal drift to align watermarks, "
                                    + "before we pause consuming from the source/task/partition.");

    public static final ConfigOption<Duration> SCAN_WATERMARK_ALIGNMENT_UPDATE_INTERVAL =
            key("scan.watermark.alignment.update-interval")
                    .durationType()
                    .defaultValue(Duration.ofSeconds(1))
                    .withDescription(
                            "How often tasks should notify coordinator about the current watermark "
                                    + "and how often the coordinator should announce the maximal aligned watermark.");

    public static final ConfigOption<Integer> SCAN_SPLIT_ENUMERATOR_BATCH_SIZE =
            key("scan.split-enumerator.batch-size")
                    .intType()
                    .defaultValue(10)
                    .withDescription(
                            "How many splits should assign to subtask per batch in StaticFileStoreSplitEnumerator "
                                    + "to avoid exceed `akka.framesize` limit.");

    public static final ConfigOption<SplitAssignMode> SCAN_SPLIT_ENUMERATOR_ASSIGN_MODE =
            key("scan.split-enumerator.mode")
                    .enumType(SplitAssignMode.class)
                    .defaultValue(SplitAssignMode.FAIR)
                    .withDescription(
                            "The mode used by StaticFileStoreSplitEnumerator to assign splits.");

    /* Sink writer allocate segments from managed memory. */
    public static final ConfigOption<Boolean> SINK_USE_MANAGED_MEMORY =
            ConfigOptions.key("sink.use-managed-memory-allocator")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription(
                            "If true, flink sink will use managed memory for merge tree; otherwise, "
                                    + "it will create an independent memory allocator.");

    /**
     * Weight of writer buffer in managed memory, Flink will compute the memory size for writer
     * according to the weight, the actual memory used depends on the running environment.
     */
    public static final ConfigOption<MemorySize> SINK_MANAGED_WRITER_BUFFER_MEMORY =
            ConfigOptions.key("sink.managed.writer-buffer-memory")
                    .memoryType()
                    .defaultValue(MemorySize.ofMebiBytes(256))
                    .withDescription(
                            "Weight of writer buffer in managed memory, Flink will compute the memory size "
                                    + "for writer according to the weight, the actual memory used depends on the running environment.");

    public static final ConfigOption<Boolean> SCAN_PUSH_DOWN =
            ConfigOptions.key("scan.push-down")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription(
                            "If true, flink will push down projection, filters, limit to the source. "
                                    + "The cost is that it is difficult to reuse the source in a job.");

    public static final ConfigOption<Boolean> SOURCE_CHECKPOINT_ALIGN_ENABLED =
            ConfigOptions.key("source.checkpoint-align.enabled")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription(
                            "Whether to align the flink checkpoint with the snapshot of the paimon table, If true, a checkpoint will only be made if a snapshot is consumed.");

    public static final ConfigOption<Duration> SOURCE_CHECKPOINT_ALIGN_TIMEOUT =
            ConfigOptions.key("source.checkpoint-align.timeout")
                    .durationType()
                    .defaultValue(Duration.ofSeconds(30))
                    .withDescription(
                            "If the new snapshot has not been generated when the checkpoint starts to trigger, the enumerator will block the checkpoint and wait for the new snapshot. Set the maximum waiting time to avoid infinite waiting, if timeout, the checkpoint will fail. Note that it should be set smaller than the checkpoint timeout.");

    public static final ConfigOption<Boolean> LOOKUP_ASYNC =
            ConfigOptions.key("lookup.async")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether to enable async lookup join.");

    public static final ConfigOption<Integer> LOOKUP_ASYNC_THREAD_NUMBER =
            ConfigOptions.key("lookup.async-thread-number")
                    .intType()
                    .defaultValue(16)
                    .withDescription("The thread number for lookup async.");

    public static List<ConfigOption<?>> getOptions() {
        final Field[] fields = FlinkConnectorOptions.class.getFields();
        final List<ConfigOption<?>> list = new ArrayList<>(fields.length);
        for (Field field : fields) {
            if (ConfigOption.class.isAssignableFrom(field.getType())) {
                try {
                    list.add((ConfigOption<?>) field.get(FlinkConnectorOptions.class));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return list;
    }

    /** Watermark emit strategy for scan. */
    public enum WatermarkEmitStrategy implements DescribedEnum {
        ON_PERIODIC(
                "on-periodic",
                "Emit watermark periodically, interval is controlled by Flink 'pipeline.auto-watermark-interval'."),

        ON_EVENT("on-event", "Emit watermark per record.");

        private final String value;
        private final String description;

        WatermarkEmitStrategy(String value, String description) {
            this.value = value;
            this.description = description;
        }

        @Override
        public String toString() {
            return value;
        }

        @Override
        public InlineElement getDescription() {
            return text(description);
        }
    }

    /**
     * Split assign mode for {@link org.apache.paimon.flink.source.StaticFileStoreSplitEnumerator}.
     */
    public enum SplitAssignMode implements DescribedEnum {
        FAIR(
                "fair",
                "Distribute splits evenly when batch reading to prevent a few tasks from reading all."),
        PREEMPTIVE(
                "preemptive",
                "Distribute splits preemptively according to the consumption speed of the task.");

        private final String value;
        private final String description;

        SplitAssignMode(String value, String description) {
            this.value = value;
            this.description = description;
        }

        @Override
        public String toString() {
            return value;
        }

        @Override
        public InlineElement getDescription() {
            return text(description);
        }
    }
}
