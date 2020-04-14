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
        new ProbeResult("OK probe", "message", ProbeResult.ProbeStatus.OK, 0));
    listener.onProbeResult(
        new ProbeResult("RETRY probe", "message", ProbeResult.ProbeStatus.RETRY, 1));
    listener.onProbeResult(
        new ProbeResult("ERROR probe", "message", ProbeResult.ProbeStatus.ERROR, 2));
    listener.onProbeResult(
        new ProbeResult("FATAL probe", "message", ProbeResult.ProbeStatus.FATAL, 3));
  }
}
