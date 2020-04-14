package io.chauvin.probes;

import org.fusesource.jansi.AnsiConsole;
import org.junit.jupiter.api.Test;

public class PrintStreamProbeResultListenerTest {
  @Test
  public void test() {
    AnsiConsole.systemInstall();

    PrintStreamProbeResultListener listener = new PrintStreamProbeResultListener(
        PrintStreamProbeResultListener.getUTF8Stdout());

    listener.onProbeResult(
        new ProbeResult("OK probe", "message", ProbeResult.Status.OK, 0));
    listener.onProbeResult(
        new ProbeResult("RETRY probe", "message", ProbeResult.Status.RETRY, 1));
    listener.onProbeResult(
        new ProbeResult("ERROR probe", "message", ProbeResult.Status.ERROR, 2));
    listener.onProbeResult(
        new ProbeResult("FATAL probe", "message", ProbeResult.Status.FATAL, 3));
  }
}
