package splitter;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SplitController extends ExcelController implements Initializable{
    @FXML
    private ChoiceBox columnBox;
    @FXML
    private Button forwardButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.forwardButton.setDisable(true);
        this.setPreviousFxml("/fxml/mode.fxml");
        this.setNextFxml("/fxml/export.fxml");
    }

    @Override
    public void setSpreadsheet(Spreadsheet spreadsheet) {
        super.setSpreadsheet(spreadsheet);
        IndexedString indexedString;

        for(int i = 0; i < spreadsheet.getColumns().size(); i++){
            indexedString = new IndexedString(i, spreadsheet.getColumns().get(i));
            columnBox.getItems().add(indexedString);
        }
    }

    public void handleChoiceBox() {
        this.forwardButton.setDisable(false);
    }

    @Override
    public void transition(Event event, String fxml){
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxml));
        try {
            Parent root = fxmlLoader.load();
            if (fxml.equals("/fxml/export.fxml")) {
                ExportController controller = fxmlLoader.getController();
                controller.setSpreadsheet(super.getSpreadsheet());
                controller.setColumn((IndexedString) columnBox.getValue());
                controller.setPreviousFxml("/fxml/split.fxml");
            } else if (fxml.equals("/fxml/mode.fxml")) {
                ModeController controller = fxmlLoader.getController();
                controller.setSpreadsheet(super.getSpreadsheet());
            }
            Stage st_old =(Stage) ((Node)event.getSource()).getScene().getWindow();
            Stage st_new = new Stage();
            st_new.setResizable(false);
            st_new.getIcons().add(new Image(Main.class.getResourceAsStream("/res/excel-splitter-small.png")));
            st_new.setTitle("Excel Splitter");
            st_new.setOnCloseRequest(e -> Platform.exit());
            Scene scene = new Scene(root);
            scene.getStylesheets().add("/css/theme.css");
            st_new.setScene(scene);
            st_new.setX(st_old.getX());
            st_new.setY(st_old.getY());
            st_old.close();
            st_new.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
