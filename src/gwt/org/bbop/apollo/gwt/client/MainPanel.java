package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.http.client.*;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.ListBox;
import org.bbop.apollo.gwt.client.dto.*;
import org.bbop.apollo.gwt.client.event.*;
import org.bbop.apollo.gwt.client.rest.OrganismRestService;
import org.bbop.apollo.gwt.client.rest.SequenceRestService;
import org.bbop.apollo.gwt.client.rest.UserRestService;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.bbop.apollo.gwt.shared.PermissionEnum;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Label;
import org.gwtbootstrap3.client.ui.constants.IconType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ndunn on 12/18/14.
 */
public class MainPanel extends Composite {


    interface MainPanelUiBinder extends UiBinder<Widget, MainPanel> {
    }


    private static MainPanelUiBinder ourUiBinder = GWT.create(MainPanelUiBinder.class);


    private boolean toggleOpen = true;
    private String rootUrl;
    public static Map<String, JavaScriptObject> annotrackFunctionMap = new HashMap<>();

    // state info
    private static UserInfo currentUser; // the current logged-in user
    // should I use a getter, or is this fine?
    static PermissionEnum highestPermission = PermissionEnum.NONE; // the current logged-in user
    private static OrganismInfo currentOrganism; // the current logged-in user
    public static Long currentOrganismId = null;
    public static String currentSequenceName = null;


    // debug
    private Boolean showFrame = false;
    private int maxUsernameLength = 15;

    @UiField
    Button dockOpenClose;
    @UiField(provided = false)
    NamedFrame frame;
    @UiField
    static AnnotatorPanel annotatorPanel;
    @UiField
    static TrackPanel trackPanel;
    @UiField
    static SequencePanel sequencePanel;
    @UiField
    static OrganismPanel organismPanel;
    @UiField
    static UserPanel userPanel;
    @UiField
    static GroupPanel userGroupPanel;
    @UiField
    static DockLayoutPanel eastDockPanel;
    @UiField
    static SplitLayoutPanel mainSplitPanel;
    @UiField
    static TabLayoutPanel detailTabs;
//    @UiField
//    static ListBox organismList;
//    @UiField(provided = true)
//    static SuggestBox sequenceList;
    @UiField
    FlowPanel westPanel;
    @UiField
    PreferencePanel preferencePanel;
    @UiField
    Button logoutButton;
    @UiField
    HTML userName;
    @UiField
    Button generateLink;
    @UiField
    org.gwtbootstrap3.client.ui.Label currentOrganismDisplay;
    @UiField
    Label currentSequenceDisplay;


//    private MultiWordSuggestOracle sequenceOracle = new MultiWordSuggestOracle();


    public MainPanel() {
        exportStaticMethod();
//        sequenceList = new SuggestBox(sequenceOracle);

        initWidget(ourUiBinder.createAndBindUi(this));


        GWT.log("name: " + frame.getName());
        frame.getElement().setAttribute("id", frame.getName());

        Dictionary dictionary = Dictionary.getDictionary("Options");
        rootUrl = dictionary.get("rootUrl");
        showFrame = dictionary.get("showFrame") != null && dictionary.get("showFrame").contains("true");


        Annotator.eventBus.addHandler(OrganismChangeEvent.TYPE, new OrganismChangeEventHandler() {
            @Override
            public void onOrganismChanged(OrganismChangeEvent organismChangeEvent) {
                switch (organismChangeEvent.getAction()) {
                    case LOADED_ORGANISMS:
//                        List<OrganismInfo> organismInfoList = organismChangeEvent.getOrganismInfoList();
//                        organismList.clear();
//                        for (OrganismInfo organismInfo : organismInfoList) {
//                            organismList.addItem(organismInfo.getName(), organismInfo.getId());
//                        }
                        break;
                    case CHANGED_ORGANISM:
                        currentOrganism = organismChangeEvent.getOrganismInfoList().get(0);
                        currentOrganismDisplay.setHTML(currentOrganism.getName());
                        updateGenomicViewer();
//                        loadReferenceSequences();
                        updatePermissionsForOrganism();
                        break;
                }
            }

        });

        Annotator.eventBus.addHandler(ContextSwitchEvent.TYPE, new ContextSwitchEventHandler() {

            @Override
            public void onContextSwitched(ContextSwitchEvent contextSwitchEvent) {
                // need to set this before calling the sequence
                currentOrganismId = Long.parseLong(contextSwitchEvent.getOrganismInfo().getId());
                currentOrganism = contextSwitchEvent.getOrganismInfo();
                currentOrganismDisplay.setHTML(currentOrganism.getName());
//                Window.alert("context switching org to "+currentOrganism.getName());

//                for (int i = 0; i < organismList.getItemCount(); i++) {
//                    organismList.setItemSelected(i, currentOrganismId.toString().equals(organismList.getValue(i)));
//                }

                String sequenceName = "";
                if (contextSwitchEvent.getSequenceInfo() != null) {
                    sequenceName = contextSwitchEvent.getSequenceInfo().getName();
//                    currentSequenceName = contextSwitchEvent.getSequenceInfo().getName();
                }

                RequestCallback requestCallback = new RequestCallback() {
                    @Override
                    public void onResponseReceived(Request request, Response response) {
//                        Window.alert("should be setting org "+currentOrganism.getName());
                        currentSequenceName= response.getText();
                        currentSequenceDisplay.setHTML(currentSequenceName);
//                        sequenceList.setText(innerSequenceName);
//                        Window.alert("going to update the genomic viewer");
                        updateGenomicViewer();
                    }

                    @Override
                    public void onError(Request request, Throwable exception) {
                        Window.alert("Error setting default sequence: " + exception);
                    }
                };
                SequenceRestService.setDefaultSequence(requestCallback, sequenceName);
            }
        });

        Annotator.eventBus.addHandler(AnnotationInfoChangeEvent.TYPE, new AnnotationInfoChangeEventHandler() {
            @Override
            public void onAnnotationChanged(AnnotationInfoChangeEvent annotationInfoChangeEvent) {
                switch (annotationInfoChangeEvent.getAction()) {
                    case SET_FOCUS:
                        AnnotationInfo annotationInfo = annotationInfoChangeEvent.getAnnotationInfo();
                        updateGenomicViewerForLocation(annotationInfo.getSequence(), annotationInfo.getMin(), annotationInfo.getMax());
                        break;
                }
            }
        });

        loginUser();
    }

    private void updatePermissionsForOrganism() {
        GWT.log(currentUser.getOrganismPermissionMap().keySet().toString());
        String globalRole = currentUser.getRole();
        GWT.log("global: " + globalRole);
        UserOrganismPermissionInfo userOrganismPermissionInfo = currentUser.getOrganismPermissionMap().get(currentOrganism.getName());
        if (globalRole.equals("admin")) {
            highestPermission = PermissionEnum.ADMINISTRATE;
        } else {
            highestPermission = PermissionEnum.NONE;
        }
        if (userOrganismPermissionInfo != null) {
            GWT.log("organism: " + userOrganismPermissionInfo.toJSON().toString());
            highestPermission = userOrganismPermissionInfo.getHighestPermission();
        }

        switch (highestPermission) {
            case ADMINISTRATE:
                GWT.log("setting to ADMINISTRATE permissions");
                detailTabs.getTabWidget(TabPanelIndex.USERS.index).getParent().setVisible(true);
                detailTabs.getTabWidget(TabPanelIndex.GROUPS.index).getParent().setVisible(true);
                detailTabs.getTabWidget(TabPanelIndex.ORGANISM.index).getParent().setVisible(true);
                detailTabs.getTabWidget(TabPanelIndex.PREFERENCES.index).getParent().setVisible(true);
                break;
            case WRITE:
                GWT.log("setting to WRITE permissions");
            case EXPORT:
                GWT.log("setting to EXPORT permissions");
            case READ:
                GWT.log("setting to READ permissions");
            case NONE:
            default:
                GWT.log("setting to no permissions");
                // let's set the view
                detailTabs.getTabWidget(TabPanelIndex.USERS.index).getParent().setVisible(false);
                detailTabs.getTabWidget(TabPanelIndex.GROUPS.index).getParent().setVisible(false);
                detailTabs.getTabWidget(TabPanelIndex.ORGANISM.index).getParent().setVisible(false);
                detailTabs.getTabWidget(TabPanelIndex.PREFERENCES.index).getParent().setVisible(false);

                break;
        }

        UserChangeEvent userChangeEvent = new UserChangeEvent(UserChangeEvent.Action.PERMISSION_CHANGED, highestPermission);
        Annotator.eventBus.fireEvent(userChangeEvent);
    }

    private void loginUser() {
        String url = rootUrl + "/user/checkLogin";
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONObject returnValue = JSONParser.parseStrict(response.getText()).isObject();
                if (returnValue.containsKey(FeatureStringEnum.USER_ID.getValue())) {
                    loadOrganisms();
                    logoutButton.setVisible(true);
                    currentUser = UserInfoConverter.convertToUserInfoFromJSON(returnValue);

                    String displayName = currentUser.getEmail();

                    userName.setHTML(displayName.length() > maxUsernameLength ?
                            displayName.substring(0, maxUsernameLength - 1) + "..." : displayName);
//                    userName.setHTML(displayName.length()>maxUsernameLength?
//                            displayName.substring(0, maxUsernameLength - 1) + "..." : displayName);
                } else {
                    boolean hasUsers = returnValue.get(FeatureStringEnum.HAS_USERS.getValue()).isBoolean().booleanValue();
                    if (hasUsers) {
                        currentUser = null;
                        logoutButton.setVisible(false);
                        LoginDialog loginDialog = new LoginDialog();
                        loginDialog.center();
                        loginDialog.show();
                    } else {
                        currentUser = null;
                        logoutButton.setVisible(false);
                        RegisterDialog registerDialog = new RegisterDialog();
                        registerDialog.center();
                        registerDialog.show();
                    }
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("User not there: " + exception);
            }
        };
        try {
            builder.setCallback(requestCallback);
            builder.send();
        } catch (RequestException e) {
            // Couldn't connect to server
            Window.alert(e.getMessage());
        }

    }


//    @UiHandler("organismList")
//    public void changeOrganism(ChangeEvent event) {
//        String selectedValue = organismList.getSelectedValue();
//        currentOrganismId = Long.parseLong(selectedValue);
////        sequenceList.setText("");
////        sequenceOracle.clear();
//        OrganismRestService.changeOrganism(selectedValue);
//    }

//    @UiHandler("sequenceList")
//    public void changeSequence(SelectionEvent<SuggestOracle.Suggestion> event) {
//        updateGenomicViewer();
//        SequenceRestService.setDefaultSequence(currentSequenceName);
//    }

    public void updateGenomicViewerForLocation(String selectedSequence, Integer minRegion, Integer maxRegion) {
        Integer buffer = (int) Math.round((maxRegion - minRegion) * 0.5);
        minRegion -= buffer;
        if (minRegion < 0) minRegion = 0;
        maxRegion += buffer;
        String trackListString = rootUrl + "/jbrowse/?loc=";
        trackListString += selectedSequence;
        trackListString += ":" + minRegion + ".." + maxRegion;
        trackListString += "&";
        for (TrackInfo trackInfo : TrackPanel.dataProvider.getList()) {
            trackListString += trackInfo.getName();
            trackListString += "&";
        }
        trackListString = trackListString.substring(0, trackListString.length() - 1);
        trackListString += "&highlight=&tracklist=0";
        GWT.log("set string: " + trackListString);
        GWT.log("get string: " + frame.getUrl());
        if (!frame.getUrl().contains(trackListString)) {
            frame.setUrl(trackListString);
        }
    }

    public void updateGenomicViewer() {
        String trackListString = rootUrl + "/jbrowse/?loc=";
//        String selectedSequence =
        GWT.log("get selected sequence: " + currentSequenceName);
        trackListString += currentSequenceName;

        trackListString += "&";
        for (TrackInfo trackInfo : TrackPanel.dataProvider.getList()) {
            trackListString += trackInfo.getName();
            trackListString += "&";
        }
        trackListString = trackListString.substring(0, trackListString.length() - 1);
        trackListString += "&highlight=&tracklist=0";
        GWT.log("set string: " + trackListString);
        frame.setUrl(trackListString);
    }

    /**
     * could use an sequence callback . . . however, this element needs to use the callback directly.
     */
//    public void loadReferenceSequences() {
//        RequestCallback requestCallback = new RequestCallback() {
//            @Override
//            public void onResponseReceived(Request request, Response response) {
////                sequenceOracle.clear();
////                sequenceList.setText("");
//                JSONValue returnValue = JSONParser.parseStrict(response.getText());
//                JSONArray array = returnValue.isArray();
//
//
//                for (int i = 0; i < array.size(); i++) {
//                    JSONObject object = array.get(i).isObject();
//                    SequenceInfo sequenceInfo = new SequenceInfo();
//                    sequenceInfo.setName(object.get("name").isString().stringValue());
//                    sequenceInfo.setStart((int) object.get("start").isNumber().doubleValue());
//                    sequenceInfo.setEnd((int) object.get("end").isNumber().doubleValue());
//                    if (object.get("default") != null) {
//                        sequenceInfo.setDefault(object.get("default").isBoolean().booleanValue());
//                    }
////                    sequenceOracle.add(sequenceInfo.getName());
//                    if (sequenceInfo.isDefault()) {
//                        GWT.log("setting name to default: " + sequenceInfo.getName());
////                        sequenceList.setText(sequenceInfo.getName());
//                        currentSequenceName = sequenceInfo.getName();
//                    }
//                    else if (currentSequenceName != null && sequenceInfo.getName().equals(currentSequenceName)) {
//                        GWT.log("setting name: " + currentSequenceName);
////                        sequenceList.setText(sequenceInfo.getName());
//                        currentSequenceName = sequenceInfo.getName();
//                    }
//                }
//
//                if (array.size() > 0) {
//                    if (currentSequenceName == null) {
//                        currentSequenceName = array.get(0).isObject().get("name").isString().stringValue();
//                    }
//                }
//                currentSequenceDisplay.setHTML(currentSequenceName);
//
//                ContextSwitchEvent contextSwitchEvent = new ContextSwitchEvent(currentSequenceName, currentOrganism);
//                Annotator.eventBus.fireEvent(contextSwitchEvent);
//            }
//
//            @Override
//            public void onError(Request request, Throwable exception) {
//                Window.alert("Error loading organisms");
//            }
//        };
//        // TODO: move to a javscript function in iFrame?
//        SequenceRestService.loadSequences(requestCallback, MainPanel.currentOrganismId);
//
//    }

    /**
     * could use an organism callback . . . however, this element needs to use the callback directly.
     *
     */
    public void loadOrganisms() {
        String url = rootUrl + "/organism/findAllOrganisms";
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                JSONArray array = returnValue.isArray();

                for (int i = 0; i < array.size(); i++) {
                    JSONObject object = array.get(i).isObject();
                    OrganismInfo organismInfo = OrganismInfoConverter.convertFromJson(object);
//                    trackInfoList.addItem(organismInfo.getName(), organismInfo.getId());
                    if (organismInfo.isCurrent()) {
                        currentOrganismId = Long.parseLong(organismInfo.getId());
                        currentOrganism = organismInfo;
                        currentOrganismDisplay.setHTML(currentOrganism.getName());
//                        trackInfoList.setSelectedIndex(i);
                    }
                }

                if (currentOrganismId == null && array.size() > 0) {
                    JSONObject rootObject = array.get(0).isObject();
                    currentOrganismId = (long) rootObject.get("id").isNumber().doubleValue();
                    currentOrganism = OrganismInfoConverter.convertFromJson(rootObject);
                    currentOrganismDisplay.setHTML(currentOrganism.getName());
//                    trackInfoList.setSelectedIndex(0);
                }
                updatePermissionsForOrganism();

                ContextSwitchEvent contextSwitchEvent = new ContextSwitchEvent(currentOrganism);
                Annotator.eventBus.fireEvent(contextSwitchEvent);
//                loadReferenceSequences();
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error loading organisms");
            }
        };
        try {
            builder.setCallback(requestCallback);
            builder.send();
        } catch (RequestException e) {
            // Couldn't connect to server
            Window.alert(e.getMessage());
        }

    }

    @UiHandler("dockOpenClose")
    void handleClick(ClickEvent event) {
        toggleOpen();
    }


    @UiHandler("detailTabs")
    public void onSelection(SelectionEvent<Integer> event) {
        reloadTabPerIndex(event.getSelectedItem());
    }

    private void reloadTabPerIndex(Integer selectedItem) {
        switch (selectedItem) {
            case 0:
                annotatorPanel.reload();
                break;
            case 1:
                trackPanel.reload();
                break;
            case 2:
                sequencePanel.reload();
                break;
            case 3:
                organismPanel.reload();
                break;
            case 4:
                userPanel.reload();
                break;
            case 5:
                userGroupPanel.reload();
                break;
            default:
                break;
        }
    }

    private void closePanel() {
        mainSplitPanel.setWidgetSize(eastDockPanel, 20);
        dockOpenClose.setIcon(IconType.CARET_LEFT);
    }

    private void openPanel() {
        mainSplitPanel.setWidgetSize(eastDockPanel, 550);
        dockOpenClose.setIcon(IconType.CARET_RIGHT);
    }

    private void toggleOpen() {
        if (mainSplitPanel.getWidgetSize(eastDockPanel) < 100) {
            toggleOpen = false;
        }

        if (toggleOpen) {
            // close
            closePanel();
        } else {
            // open
            openPanel();
        }

        mainSplitPanel.animate(400);

        toggleOpen = !toggleOpen;
    }


    public void setRootUrl(String rootUrl) {
        this.rootUrl = rootUrl;
    }


    public static void registerFunction(String name, JavaScriptObject javaScriptObject) {
        annotrackFunctionMap.put(name, javaScriptObject);
    }

    @UiHandler("generateLink")
    public void generateLink(ClickEvent clickEvent) {
        UrlDialogBox urlDialogBox = new UrlDialogBox(frame.getUrl());
        urlDialogBox.setWidth("600px");

        urlDialogBox.show();
    }

    @UiHandler("logoutButton")
    public void logout(ClickEvent clickEvent) {
        UserRestService.logout();
    }

    public static UserInfo getCurrentUser() {
        return currentUser;
    }

    public static String executeFunction(String name) {
        return executeFunction(name, JavaScriptObject.createObject());
    }

    public static String executeFunction(String name, JavaScriptObject dataObject) {
        JavaScriptObject targetFunction = annotrackFunctionMap.get(name);
        if (targetFunction == null) {
            return "function " + name + " not found";
        }
        return executeFunction(targetFunction, dataObject);
    }


    public static native String executeFunction(JavaScriptObject targetFunction, JavaScriptObject data) /*-{
        return targetFunction(data);
    }-*/;


    public static void reloadAnnotator() {
        GWT.log("!!! MainPanel::calling annotator relaod ");
        annotatorPanel.reload();
    }

    public static void reloadSequences() {
        sequencePanel.reload();
    }

    public static void reloadOrganisms() {
        organismPanel.reload();
    }

    public static void reloadUsers() {
        userPanel.reload();
    }

    public static void reloadUserGroups() {
        userGroupPanel.reload();
    }

    public static native void exportStaticMethod() /*-{
        $wnd.reloadAnnotations = $entry(@org.bbop.apollo.gwt.client.MainPanel::reloadAnnotator());
        $wnd.reloadSequences = $entry(@org.bbop.apollo.gwt.client.MainPanel::reloadSequences());
        $wnd.reloadOrganisms = $entry(@org.bbop.apollo.gwt.client.MainPanel::reloadOrganisms());
        $wnd.reloadUsers = $entry(@org.bbop.apollo.gwt.client.MainPanel::reloadUsers());
        $wnd.reloadUserGroups = $entry(@org.bbop.apollo.gwt.client.MainPanel::reloadUserGroups());
        $wnd.registerFunction = $entry(@org.bbop.apollo.gwt.client.MainPanel::registerFunction(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;));
        $wnd.getEmbeddedVersion = $entry(
            function apolloEmbeddedVersion() {
                return 'ApolloGwt-1.0';
            }
        );
    }-*/;

    private enum TabPanelIndex {
        ANNOTATIONS(0),
        TRACKS(1),
        SEQUENCES(2),
        ORGANISM(3),
        USERS(4),
        GROUPS(5),
        PREFERENCES(6),;

        private int index;

        TabPanelIndex(int index) {
            this.index = index;
        }

    }

    public static boolean isCurrentUserAdmin() {
        return (currentUser != null && currentUser.getRole().equals("admin"));
    }

}
