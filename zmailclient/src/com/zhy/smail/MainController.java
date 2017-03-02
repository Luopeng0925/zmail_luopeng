package com.zhy.smail;

import com.zhy.smail.cabinet.entity.CabinetInfo;
import com.zhy.smail.cabinet.service.CabinetService;
import com.zhy.smail.common.json.JsonResult;
import com.zhy.smail.component.SimpleDialog;
import com.zhy.smail.component.music.Speaker;
import com.zhy.smail.config.GlobalOption;
import com.zhy.smail.config.LocalConfig;
import com.zhy.smail.restful.RestfulResult;
import com.zhy.smail.restful.RfFaultEvent;
import com.zhy.smail.restful.RfResultEvent;
import com.zhy.smail.setting.service.OptionService;
import com.zhy.smail.user.entity.UserInfo;
import com.zhy.smail.user.service.UserService;
import com.zhy.smail.user.view.LoginController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable{
    @FXML
    private Label lblMessage;
    @FXML
    private Label lblAppTitle;

    private String typedStr;
    private boolean startGetTyped;

    public void initialize(URL location, ResourceBundle resources){
        OptionService.loadOptions(new RestfulResult() {
            @Override
            public void doResult(RfResultEvent event) {

            }

            @Override
            public void doFault(RfFaultEvent event) {

            }
        });
        lblMessage.setVisible(false);

        startGetTyped = false;
    }

    @FXML
    public void onScreenMax(ActionEvent action){
        app.getRootStage().setMaximized(true);
    }



    @FXML
    protected void onExitAction(ActionEvent event) {

        Platform.exit();
    }

    private MainApp app;

    public void setApp(MainApp app){
        this.app = app;
    }

    public void setAppTitle(String title){
        lblAppTitle.setText(title);
    }

    @FXML
    private void onLoginAction(ActionEvent event) throws IOException {
       app.goLogin(false);
    }

    @FXML
    private void onDeliveryAction(ActionEvent event) throws IOException{
        app.goLogin(true);
    }

    @FXML
    private void onHelpAction(ActionEvent event) throws IOException{
        app.goHelp();
    }

    @FXML
    private void onKeyPressed(KeyEvent event){
        if(event.getCode() == KeyCode.SEMICOLON){
            startGetTyped = true;
            typedStr = "";
        }
        else if(startGetTyped && event.getCode() == KeyCode.ENTER){
            startGetTyped = false;
            startToLogin(typedStr);
        }
        else if(startGetTyped){
            typedStr += event.getText();
        }
    }

    private void startToLogin(String cardNo){
        UserService.getByCardNo(cardNo, new RestfulResult() {
            @Override
            public void doResult(RfResultEvent event) {
                if(event.getResult() == JsonResult.FAIL){
                    Speaker.invalideCard();
                    SimpleDialog.showMessageDialog(app.getRootStage(), "无效卡", "");
                }
                else {
                    UserInfo user = (UserInfo) event.getData();
                    GlobalOption.currentUser = user;
                    LoginController loginController = null;
                    switch (user.getUserType()) {
                        case UserInfo.FACTORY_USER:
                        case UserInfo.ADMIN:
                        case UserInfo.ADVANCED_ADMIN:
                            loginController = app.goLogin(false);
                            break;
                        case UserInfo.DELIVERY:
                        case UserInfo.MAILMAN:
                            loginController = app.goLogin(true);
                            break;
                        case UserInfo.OWNER:
                            if(GlobalOption.cardNeedPassword.getIntValue() == 0) {
                                getCurrentCabient("本地箱柜号没有设置,请联系系统管理员。");
                                app.goOwner();
                                return;
                            }
                            else{
                                loginController = app.goLogin(false);
                            }
                            break;
                    }
                    if(loginController != null){
                        loginController.setUserName(cardNo);
                        loginController.passwordFocus();
                    }
                }

            }

            @Override
            public void doFault(RfFaultEvent event) {

            }
        });
    }

    private void getCurrentCabient(String message){
        String localCabinet = LocalConfig.getInstance().getLocalCabinet();
        if(localCabinet == null || localCabinet.length()==0){
            SimpleDialog.showMessageDialog(app.getRootStage(), message,"错误");
        }
        else{
            CabinetService.getByCabinetNo(localCabinet, new RestfulResult() {
                @Override
                public void doResult(RfResultEvent event) {
                    if(event.getResult() == RfResultEvent.OK){
                        GlobalOption.currentCabinet = (CabinetInfo)event.getData();
                    }
                    else{
                        SimpleDialog.showMessageDialog(app.getRootStage(), message,"错误");
                    }
                }

                @Override
                public void doFault(RfFaultEvent event) {

                }
            });
        }
    }
}