package view;

import java.util.List;

import controller.IRobotController;
import controller.RobotController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import lejos.robotics.geometry.Line;
import lejos.robotics.mapping.LineMap;
import lejos.robotics.navigation.Pose;
import model.RobotConfig;
import utils.Constants;

/**
 * A graphical user interface that implements @{@link IRobotUI}.
 */
public class RobotGUI extends Application implements IRobotUI {

    private static final String host = "10.0.1.1";
    private static final int port = 1111;

    private IRobotController controller;

    private ImageView webcamView;
    private Map map;
    private Label coordLabel;
    private Label message;
    private Label titleLbl;

    private RobotConfig conf;

    /*
     * Initializes the controller and sets default configuration for robot. 
     * @see {@link RobotConfig}
     */
    @Override
    public void init() {
	controller = new RobotController(this);

	double diameter = 4.15;
	double offset = 6.49;
	conf = new RobotConfig("default", diameter, offset);
    }

    @Override
    public void start(Stage window) throws Exception {
	webcamView = new ImageView();
	webcamView.setFitWidth(640);
	webcamView.setFitHeight(480);
	map = new Map(testMap(), new Pose(20, 20, 0));
	coordLabel = new Label("y=20, x=20");
	message = new Label("");

	BorderPane root = new BorderPane();
	String style = 
	    "-fx-font-size: 14pt;"+
	    "-fx-font-family: \"Courier New\";";
	root.setStyle(style);	

	GridPane movementBtns = movementButtons();
	GridPane center = new GridPane();	
	
	ToggleButton navigation = new ToggleButton("Navigation");	
	MapArea mapArea = new MapArea(controller, map, navigation);
	
	navigation.setOnAction(evt -> {
		Platform.runLater(() -> {			
			titleLbl.setText("Navigation setup");
			mapArea.toggleNavMode();
		});
	});
	
	center.add(navigation, 0, 0);
	center.add(movementBtns, 0, 1);	
	center.add(coordLabel, 0, 2);
	center.add(message, 1, 3);
	center.add(mapArea, 1, 0);
	center.add(mapArea.getButtons(), 1, 2);

	center.setPadding(new Insets(20, 20, 20, 20));
	coordLabel.setPadding(new Insets(10, 10, 10, 10));	
	GridPane.setRowSpan(mapArea, 2);
	movementBtns.setAlignment(Pos.BOTTOM_CENTER);

	root.setTop(top());
	root.setCenter(center);

	Button resetBtn = new Button("Reset position");
	resetBtn.setOnMouseClicked(evt -> {
	    Platform.runLater(() -> {
		map.redraw(new Pose(20, 20, 0));
	    });
	});

	Button configButton = new Button("Robot config");
	configButton.setOnMouseClicked(evt -> {
	    Stage configWindow = new Stage();

	    HBox croot = new HBox();

	    ListView<String> configList = new ListView<>();
	    List<RobotConfig> configs = controller.getConfigs();

	    if (configs == null) {
		croot
		    .getChildren()
		    .addAll(new Label("loading configs unsupported.\ndatabase connection failed."));
	    } else {
		ObservableList<String> listv = FXCollections.observableArrayList();
		configs.forEach(c -> listv.add(c.toString()));
		configList.setItems(listv);

		Button selectConfBtn = new Button("set");
		selectConfBtn.setOnMouseClicked(evt2 -> {
		    int i = configList.getSelectionModel().getSelectedIndex();
		    if (i == -1) return;
		    conf = configs.get(i);
		    System.out.println("set: "+conf);
		});

		croot.getChildren().addAll(configList, selectConfBtn);
	    }
	    configWindow.setScene(new Scene(croot));
	    configWindow.setTitle("Robot configuration");
	    configWindow.show();
	});
	VBox resetAndconfBtns = new VBox(); 
	resetAndconfBtns.getChildren().addAll(resetBtn, configButton);
	center.add(resetAndconfBtns, 3, 3);


	Scene scene = new Scene(root);
	window.setScene(scene);
	window.setTitle("Robotti GUI");
	window.show();
    }
	

    @Override
    public void updateVideo(Image image) {
	Platform.runLater(() -> {
	    webcamView.setImage(image);
	});
    }

    @Override
    public void updateMap(Pose pose) {
	Platform.runLater(() -> {
	    double y = pose.getY();
	    double x = pose.getX();
	    String labelText = String.format("y=%.2f, x=%.2f", y, x);
	    coordLabel.setText(labelText);
	    map.redraw(pose);
	});
    }

    @Override
    public void setMessage(String msg) {
	Platform.runLater(() -> {
	    message.setText(msg);
	});
    }

    private Parent top() {
	BorderPane topArea = new BorderPane();
	topArea.setPadding(new Insets(5, 5, 5, 5));
	topArea.setLeft(topLeft());
	topArea.setRight(topRight());
	return topArea;
    }

    private Parent topRight() {
	HBox topRightArea = new HBox();
	topRightArea.setPadding(new Insets(5, 5, 5, 5));
	topRightArea.setSpacing(20);

	Button videoButton = new Button("📹 Webcam");

	videoButton.setOnAction(evt -> {
	    Platform.runLater(() -> {
		Stage webcamWindow = new Stage();
		webcamWindow.setMinWidth(640);
		webcamWindow.setMinHeight(480);

		StackPane pane = new StackPane();
		pane.getChildren().addAll(webcamView);

		webcamWindow.setScene(new Scene(pane));
		webcamWindow.setTitle("EV3 webcam");

		webcamWindow.show();
	    });
	});
	
	topRightArea.getChildren().addAll(videoButton);
	return topRightArea;
    }

    private Parent topLeft() {
	HBox topLeftArea = new HBox();
	topLeftArea.setPadding(new Insets(5, 5, 5, 5));
	topLeftArea.setSpacing(20);

	String red = "#FF0000";
	String green = "#00FF00";

	Button connectBtn = new Button("Connect to EV3");

	Label connectedIcon = new Label("⬤");
	connectedIcon.setTextFill(Color.web(red, 1));
	titleLbl = new Label("");

	connectBtn.setOnAction((evt) -> {
	    if (controller.isConnected()) {
		controller.disconnect();
		message.setText("Disconnected");
		connectedIcon.setTextFill(Color.web(red, 1));
		message.setTextFill(Color.web(red, 1));
		connectBtn.setText("Connect to EV3");
	    } else {
		int timeout = 1000;
		boolean success = controller.connect(host, port, timeout);
		if (!success) {
		    return;
		}

		controller.sendConfig(conf);

		message.setText("Connected");
		message.setTextFill(Color.web(green, 1));
		connectedIcon.setTextFill(Color.web(green, 1));
		connectBtn.setText("Disconnect EV3");
	    }
	});

	topLeftArea.getChildren().addAll(connectedIcon, connectBtn, titleLbl);
	return topLeftArea;
    }

    private GridPane movementButtons() {
	GridPane btnGrid = new GridPane();
	btnGrid.setPadding(new Insets(20, 20, 20, 20));

	Button up = createMovementButton(Constants.MOVE_ROBOT_FORWARD, "⮸");
	Button down = createMovementButton(Constants.MOVE_ROBOT_BACKWARD, "⮸");
	down.setRotate(180);
	Button left = createMovementButton(Constants.TURN_ROBOT_LEFT, "⮶");
	Button right = createMovementButton(Constants.TURN_ROBOT_RIGHT, "⮷");
	
	btnGrid.add(up, 1, 0);
	btnGrid.add(down, 1, 1);
	btnGrid.add(left, 0, 1);
	btnGrid.add(right, 2, 1);

	return btnGrid;
    }

    private Button createMovementButton(int direction, String label) {
	Button btn = new Button(label);

	btn.setOnMousePressed(evt -> {
	    controller.moveRobot(direction);
	});
	btn.setOnMouseReleased(evt -> {
	    controller.stopRobot();
	});

	return btn;
    }

    private static LineMap testMap() {
	lejos.robotics.geometry.Rectangle alue =
	    new lejos.robotics.geometry.Rectangle(0, 0, 150, 150);
        Line[] esteet = new Line[12];

        // borders	
        esteet[0] = new Line(0, 0, 150, 0);
        esteet[1] = new Line(150, 0, 150, 150);
        esteet[2] = new Line(0, 150, 150, 150);
        esteet[3] = new Line(0, 0, 0, 150);

        // obstacle 1
        esteet[4] = new Line(50, 40, 60, 40);
        esteet[5] = new Line(60, 40, 60, 110);
        esteet[6] = new Line(50, 110, 60, 110);
        esteet[7] = new Line(50, 40, 50, 110);

        // obstacle 2
        esteet[8] = new Line(100, 40, 110, 40);
        esteet[9] = new Line(110, 40, 110, 110);
        esteet[10] = new Line(100, 110, 110, 110);
        esteet[11] = new Line(100, 40, 100, 110);

        return new LineMap(esteet, alue);
    }

}
