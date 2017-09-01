package osgi.enroute.iot.lego.adapter;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import osgi.enroute.iot.gpio.api.CircuitBoard;
import osgi.enroute.iot.gpio.util.ICAdapter;
import osgi.enroute.iot.gpio.util.Wave;

/**
 * http://www.philohome.com/pf/LEGO_Power_Functions_RC_v120.pdf
 *
 */
public class LegoRC extends ICAdapter<LegoPowerFunctions, Wave> implements
        LegoPowerFunctions {

    Wave wave;

    //
    // A pulse is 1/38.000 sec = 26.3 µSec
    // We need 6 pulses = 158 µSec
    final int[] PULSES = { //
            13, // 13 13.157894736842104
            13, // 26 26.31578947368421
            13, // 39 39.473684210526315
            14, // 53 52.63157894736842
            13, // 66 65.78947368421052
            13, // 79 78.94736842105263
            13, // 92 92.10526315789473
            13, // 105 105.26315789473684
            13, // 118 118.42105263157895
            14, // 132 131.57894736842104
            13, // 145 144.73684210526315
                // 158 µSec
    };

    // From the specification:

    final int START_STOP = 1184 - 158; // 6 + 39 = 45 x 1/38K = 1184 us/
    final int LOW = 421 - 158; // 6 + 10 = 16 x 1/38K = 421 us
    final int HIGH = 711 - 158; // 6 + 21 = 27 x 1/38K = 711 us

    final static int ADDRESS = 0b0000_0000_0000_0000;
    final static int ESCAPE = 0b0100_0000_0000_0000;
    final static int SINGLE_OUTPUT_MODE_A = 0b0000_0100_0000_0000;
    final static int SINGLE_OUTPUT_MODE_B = 0b0000_0101_0000_0000;
    final static int TOGGLE = 0b1000_0000_0000_0000;

    boolean toggle = false;
    int channel;
    int values[] = new int[2];
    Lock lock = new ReentrantLock();

    int[] pulses = new int[1000];
    int where = 0;

    protected void activate(Channel channel) throws Exception {
        this.channel = channel.ordinal();
    }

    @Override
    public void A(Double speed) throws Exception {
        send(speed, 0, SINGLE_OUTPUT_MODE_A);
    }

    @Override
    public void B(Double speed) throws Exception {
        send(speed, 1, SINGLE_OUTPUT_MODE_B);
    }

    @Override
    public String getName() {
        return "LRC-CH" + (channel + 1);
    }

    void sendpulse(int width) {
        System.arraycopy(PULSES, 0, pulses, where, PULSES.length);
        where += PULSES.length;
        pulses[where++] = width;
    }

    // Calculate and insert LRC check bits
    // Send 16 bit word with start and stop
    private void sendword(int data) throws Exception {
        if (toggle) {
            data |= TOGGLE;
        }
        toggle = !toggle;

        // Calculate check LRC bits
        int check = 0xf ^ (data >> 12) ^ (data >> 8) ^ (data >> 4);

        // Insert into data nibble 4
        data &= ~0xf;
        data |= check & 0xf;

        // System.out.printf("send %04x\n", data);

        where = 0;
        sendpulse(START_STOP);

        for (int i = 0; i < 16; i++) {
            if ((data & 0x8000) != 0)
                sendpulse(HIGH);
            else
                sendpulse(LOW);

            data <<= 1;
        }
        sendpulse(START_STOP);

        int[] result = new int[where - 1];
        System.arraycopy(pulses, 0, result, 0, where - 1);

        long now = System.currentTimeMillis();
        int tm = 16;

        for (int i = 0; i < 5; i++) {
            long delay;

            switch (i) {
            default:
            case 0:
                delay = 0;
                break;

            // (4 – Ch)*tm
            case 1:
                delay = 5 * tm;
                break;

            case 2:
                delay = (4 - channel) * tm;
                break;

            // The time from start to start for the following messages is:
            // (6 + 2*Ch)*tm
            case 3:
            case 4:
                delay = (6 + 2 * channel) * tm;
            }

            long now2 = System.currentTimeMillis();
            delay = now + delay - now2;
            if (delay > 0) {
                Thread.sleep(delay);
                now = now2;
            }

            if (wave != null)
                wave.send(result);
            else
                out().send(result);
        }

    }

    void send(double value, int index, int singleOutputMode) throws Exception {
        value = Math.max(-1, value);
        value = Math.min(1, value);
        int vint = (int) Math.round(value * 7) & 0xF;

        if (index == 0) {  // only debug channel A == move()
            System.out.printf("%s.%c speed=%d (%.0f%%)\n",
                    getName(), (index == 0 ? 'A' : 'B'), vint, value * 100);
        }

        
        if (vint == 0) {
            // send brake, then float
            send(8, index, singleOutputMode);
        }

        send(vint, index, singleOutputMode);
    }
    
    void send(int vint, int index, int mode) throws Exception {
        lock.lock();
        try {
            values[index] = vint;
            int data = ADDRESS | (channel << 12) | mode | (vint << 4);
            sendword(data);
        } finally {
            lock.unlock();
        }


    }

    public void setCircuitBoard(CircuitBoard cb) {
        super.setCircuitBoard(cb);
    }

    public void setWave(Wave wave) {
        this.wave = wave;
    }

}
