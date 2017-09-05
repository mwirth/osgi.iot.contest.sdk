package osgi.enroute.trains.train.api;

/**
 * A Train Controller interfaces to the Lego IR interface.
 */
public interface TrainController {

	/**
     * Service property identifying train for this controller
     */
	String CONTROLLER_TRAIN = "controller.train";
	
	/**
     * Service property identifying RC channel for this controller
     */
    String CONTROLLER_CHANNEL = "controller.channel";

	/**
	 * Control the motor. Positive is forward, negative is reverse, 0 is stop.
	 * 
	 * @param directionAndSpeed
	 */
	void move(int directionAndSpeed);

	/**
	 * Control the light on the train
	 * @param on
	 */
	void light(boolean on);
	
}
