package cc.etherspace.calladapter;

import cc.etherspace.*;
import rx.Observable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public interface RxGreeter {
    @Send
    Observable<TransactionReceipt> newGreeting(String greeting);

    @Call
    Observable<String> greet();

    @SuppressWarnings("unused")
    class Modified {
        private SolBytes32 oldGreetingIdx;
        private SolBytes32 newGreetingIdx;
        private String oldGreeting;
        private String newGreeting;

        @EventConstructor
        public Modified(@Indexed(argumentType = String.class) SolBytes32 oldGreetingIdx, @Indexed(argumentType = String.class) SolBytes32 newGreetingIdx, String oldGreeting, String newGreeting) {
            this.oldGreetingIdx = oldGreetingIdx;
            this.newGreetingIdx = newGreetingIdx;
            this.oldGreeting = oldGreeting;
            this.newGreeting = newGreeting;
        }

        public SolBytes32 getOldGreetingIdx() {
            return oldGreetingIdx;
        }

        public void setOldGreetingIdx(SolBytes32 oldGreetingIdx) {
            this.oldGreetingIdx = oldGreetingIdx;
        }

        public SolBytes32 getNewGreetingIdx() {
            return newGreetingIdx;
        }

        public void setNewGreetingIdx(SolBytes32 newGreetingIdx) {
            this.newGreetingIdx = newGreetingIdx;
        }

        public String getOldGreeting() {
            return oldGreeting;
        }

        public void setOldGreeting(String oldGreeting) {
            this.oldGreeting = oldGreeting;
        }

        public String getNewGreeting() {
            return newGreeting;
        }

        public void setNewGreeting(String newGreeting) {
            this.newGreeting = newGreeting;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Modified modified = (Modified) o;
            return Objects.equals(oldGreetingIdx, modified.oldGreetingIdx) &&
                    Objects.equals(newGreetingIdx, modified.newGreetingIdx);
        }

        @Override
        public int hashCode() {

            return Objects.hash(oldGreetingIdx, newGreetingIdx);
        }
    }
}