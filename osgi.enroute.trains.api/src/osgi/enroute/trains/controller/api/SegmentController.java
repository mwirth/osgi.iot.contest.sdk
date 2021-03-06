package osgi.enroute.trains.controller.api;

/**
 * A controller controls a signal, a switch and an RFID reader.
 */
public interface SegmentController {

	/**
	 * Service property for identifying this controller
	 */
	String CONTROLLER_ID = "controller.id";

	/**
	 * Service property for identifying this controller, by segment name
	 */
	String CONTROLLER_SEGMENT = "controller.segment";

	/**
	 * Get the host location of the segment controller.
	 */
	default String getLocation() {
		return "unknown";
	}

}
