package controller;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

import io.DataIn;
import lejos.robotics.pathfinding.Path;
import model.IRobotConfigDAO;
import model.RobotConfig;
import model.RobotConfigDAOHibernate;
import view.IRobotUI;
import utils.Constants;

public class RobotController implements IRobotController {

	private IRobotUI ui;
	private Socket socket;
	private DataIn in;
	private DataOutputStream out;

	private IRobotConfigDAO dao;

	public RobotController(IRobotUI ui) {
		this.ui = ui;
		this.socket = null;
		this.in = null;
		this.out = null;
		this.dao = null;

		try {
			dao = new RobotConfigDAOHibernate();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	@Override
	public boolean connect(String host, int port) {
		if (isConnected())
			return false;

		try {
			socket = new Socket(host, port);
			out = new DataOutputStream(socket.getOutputStream());
			DataIn in = new DataIn(socket, ui);

			in.start();
			return true;
		} catch (IOException ex) {
			ui.setMessage("Connection failed.");
			socket = null;
			return false;
		} catch (Exception ex) {
			ui.setMessage(ex.getMessage());
			socket = null;
			return false;
		}
	}

	@Override
	public boolean connect(String host, int port, int timeout) {
		if (isConnected())
			return false;

		try {
			socket = new Socket();
			socket.connect(new InetSocketAddress(host, port), timeout);
			out = new DataOutputStream(socket.getOutputStream());
			DataIn in = new DataIn(socket, ui);

			in.start();
			return true;
		} catch (IOException ex) {
			ui.setMessage("Connection failed.");
			socket = null;
			return false;
		} catch (Exception ex) {
			ui.setMessage(ex.getMessage());
			socket = null;
			return false;
		}
	}

	@Override
	public void disconnect() {
		if (!isConnected())
			return;

		try {
			socket.close();
		} catch (IOException ex) {
			ui.setMessage(ex.getMessage());
		}
		try {
			in.exit();
		} catch (IOException ex) {
			ui.setMessage(ex.getMessage());
		} catch (Exception ex) {
		}
		socket = null;
	}

	@Override
	public boolean isConnected() {
		return socket != null;
	}

	@Override
	public void moveRobot(int direction) {
		if (!isConnected())
			return;

		try {
			out.writeInt(direction);
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
			ui.setMessage(ex.getMessage());
		}
	}

	@Override
	public void stopRobot() {
		if (!isConnected())
			return;

		try {
			out.writeInt(Constants.STOP);
		} catch (IOException ex) {
			ui.setMessage(ex.getMessage());
		}
	}

	@Override
	public void sendWaypoints(Path wps) {
		if (!isConnected())
			return;

		try {
			out.writeInt(6);
			wps.dumpObject(out);
		} catch (Exception e) {
		}
	}

	@Override
	public List<RobotConfig> getConfigs() {
		if (dao == null) {
			return null;
		}
		return dao.readConfigs();
	}

	@Override
	public void saveConfig(RobotConfig config) {
		if (dao != null) {
			dao.createConfig(config);
		}
	}

	@Override
	public void sendConfig(RobotConfig config) {
		if (!isConnected()) {
			return;
		}

		try {
			out.writeDouble(config.getDiameter());
			out.writeDouble(config.getOffset());
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}

}
