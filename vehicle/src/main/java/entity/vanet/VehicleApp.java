package entity.vanet;

import globals.Resources;
import remote.RemoteVehicleInterface;
import remote.RemoteVehicleNetworkInterface;
import remote.Vector2Df;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Hello world!
 *
 */
public class VehicleApp {
	public static final String VEHICLE_ARGS_USAGE = "vehicle <VIN> <posX> <posY> <velX> <velY>";

	public static void main(String[] args) {
		System.out.println("\n");

		Vehicle vehicle;

		// Parse args
		if(args.length == 0) {
			System.out.println(Resources.NOTIFY_MSG("Assuming random values for position, velocity and VIN."));
			System.out.println(Resources.NOTIFY_MSG("To specify this values you could call with: " + VEHICLE_ARGS_USAGE + "."));
// FIX ASAP !!!
			vehicle = new Vehicle("VIN1", "vehicle1", new Vector2Df(0, 0), new Vector2Df(0, 0)); // < --- FIXME.. PLS
// FIX ASAP !!!

		} else if(args.length == 6) {
			try {
				float px = Float.parseFloat(args[2]);
				float py = Float.parseFloat(args[3]);
				float vx = Float.parseFloat(args[4]);
				float vy = Float.parseFloat(args[5]);
				vehicle = new Vehicle(args[0], args[1], new Vector2Df(px, py), new Vector2Df(vx, vy));
			} catch (NumberFormatException e) {
				System.out.println(Resources.ERROR_MSG("Received the correct amount of arguments but couldn't convert to float."));
				return;
			}
		} else {
			System.out.println(Resources.ERROR_MSG("Incorrect amount of arguments."));
			System.out.println(Resources.NOTIFY_MSG("Usage: " + VEHICLE_ARGS_USAGE));
			return;
		}
		System.out.println(Resources.OK_MSG("Started: " + vehicle));

		// Create registry if it doesn't exist
		try {
			LocateRegistry.createRegistry(Resources.REGISTRY_PORT);
		} catch(Exception e) {
			// registry is already created
		}

		// Connect to the VANET
		RemoteVehicleNetworkInterface VANET;
		String vehicleUniqueName;
		try {
			Registry registry = LocateRegistry.getRegistry(Resources.REGISTRY_PORT);
			VANET = (RemoteVehicleNetworkInterface) registry.lookup(Resources.VANET_NAME);
			// Get a unique name from the VANET
			vehicleUniqueName = VANET.getNextVehicleName();
		} catch(Exception e) {
			System.err.println(Resources.ERROR_MSG("Failed to connect to VANET: " +  e.getMessage()));
			System.exit(0); // Return seems to not work for some reason
			return;
		}

		// Publish remote vehicle
		RemoteVehicleService remoteVehicle = new RemoteVehicleService(vehicle, vehicleUniqueName);
		remoteVehicle.publish();

		// Start the vehicle
		vehicle.start(VANET, vehicleUniqueName);

		// Add vehicle to the VANET
		try {
			boolean result = VANET.addVehicle(vehicleUniqueName);
			if(result == false) {
				throw new Exception("Remote call to the VANET to add this vehicle failed.");
			}
		} catch(Exception e) {
			System.err.println(Resources.ERROR_MSG("Failed add vehicle to the VANET: " +  e.getMessage()));
			System.exit(0); // Return seems to not work for some reason
			return;
		}

		// Handle wait and removal
		try {
			System.out.println("Press enter to kill the vehicle.");
			System.in.read();
			VANET.removeVehicle(vehicleUniqueName);
		} catch (java.io.IOException e) {
			System.out.println(Resources.ERROR_MSG("Unable to read from input. Exiting."));
		} finally {
			remoteVehicle.unpublish();
			System.out.println("\n");
			System.exit(0);
		}
	}
}
