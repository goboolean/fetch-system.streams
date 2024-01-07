package io.goboolean.streams.streams;

import io.goboolean.streams.serde.Model;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;

import java.time.ZonedDateTime;
import java.util.ArrayList;

public class TradeTransformer implements Transformer<Integer, Model.Trade, KeyValue<Integer, Model.Aggregate>> {
    private ProcessorContext context;

    private ArrayList<Model.Trade> trades = new ArrayList<>();
    private ZonedDateTime roundedTime;

    private TimeTruncator.Truncator truncator = new TimeTruncator.OneSecTruncator();

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
    }

    @Override
    public KeyValue<Integer, Model.Aggregate> transform(Integer key, Model.Trade value) {
        ZonedDateTime givenRoundedTime = truncator.truncate(value.timestamp());
        if (roundedTime == null) { // Case when the first record is received
            roundedTime = givenRoundedTime;
            trades.add(value);
            return null;
        }

        if (roundedTime.equals(givenRoundedTime)) { // Case when the time has not changed
            trades.add(value);
            return null;
        }

        else { // Case when the time has changed
            long volume = this.trades.stream()
                    .mapToLong(Model.Trade::volume)
                    .sum();
            float average = (float) this.trades.stream()
                    .mapToDouble(a -> a.price() * a.volume())
                    .sum() / volume;
            float min = this.trades.stream()
                    .map(Model.Trade::price)
                    .min(Float::compareTo)
                    .get();
            float max = this.trades.stream()
                    .map(Model.Trade::price)
                    .max(Float::compareTo)
                    .get();

            Model.Aggregate aggregate = new Model.Aggregate(
                    value.symbol(),
                    trades.get(0).price(),
                    trades.get(trades.size() - 1).price(),
                    max,
                    min,
                    average,
                    volume,
                    roundedTime
            );

            trades.clear();
            trades.add(value);
            roundedTime = givenRoundedTime;

            return KeyValue.pair(key, aggregate);
        }
    }

    @Override
    public void close() {}
}
