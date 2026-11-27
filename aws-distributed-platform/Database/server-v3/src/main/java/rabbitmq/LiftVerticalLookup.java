package rabbitmq;

import java.util.HashMap;
import java.util.Map;

public class LiftVerticalLookup {

  private static final Map<Integer,Integer> verticals = new HashMap<>();

  static {
    verticals.put(1, 167);
    verticals.put(2, 74);
    verticals.put(3, 344);
    verticals.put(4, 273);
    verticals.put(5, 383);
    verticals.put(6, 397);
    verticals.put(7, 104);
    verticals.put(8, 88);
    verticals.put(9, 288);
    verticals.put(10, 183);
    verticals.put(11, 204);
    verticals.put(12, 214);
    verticals.put(13, 222);
    verticals.put(14, 104);
    verticals.put(15, 364);
    verticals.put(16, 357);
    verticals.put(17, 213);
    verticals.put(18, 336);
    verticals.put(19, 173);
    verticals.put(20, 223);
    verticals.put(21, 267);
    verticals.put(22, 315);
    verticals.put(23, 250);
    verticals.put(24, 200);
    verticals.put(25, 368);
    verticals.put(26, 181);
    verticals.put(27, 223);
    verticals.put(28, 88);
    verticals.put(29, 138);
    verticals.put(30, 99);
    verticals.put(31, 262);
    verticals.put(32, 213);
    verticals.put(33, 70);
    verticals.put(34, 125);
    verticals.put(35, 340);
    verticals.put(36, 63);
    verticals.put(37, 296);
    verticals.put(38, 103);
    verticals.put(39, 374);
    verticals.put(40, 269);
  }

  public static int getVertical(int liftId) {

    return verticals.getOrDefault(liftId, 0);
  }
}