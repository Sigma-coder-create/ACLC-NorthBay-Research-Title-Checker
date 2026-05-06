package raven.modal.demo.menu;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import raven.extras.AvatarIcon;
import raven.modal.demo.Demo;
import raven.modal.demo.forms.*;
import raven.modal.demo.model.ModelUser;
import raven.modal.demo.system.AllForms;
import raven.modal.demo.system.Form;
import raven.modal.demo.system.FormManager;
import raven.modal.drawer.DrawerPanel;
import raven.modal.drawer.item.Item;
import raven.modal.drawer.item.MenuItem;
import raven.modal.drawer.menu.MenuOption;
import raven.modal.drawer.menu.MenuStyle;
import raven.modal.drawer.renderer.DrawerStraightDotLineStyle;
import raven.modal.drawer.simple.SimpleDrawerBuilder;
import raven.modal.drawer.simple.footer.LightDarkButtonFooter;
import raven.modal.drawer.simple.footer.SimpleFooterData;
import raven.modal.drawer.simple.header.SimpleHeader;
import raven.modal.drawer.simple.header.SimpleHeaderData;
import raven.modal.option.Option;


import javax.swing.*;
import java.util.Arrays;

public class MyDrawerBuilder extends SimpleDrawerBuilder {

    private static MyDrawerBuilder instance;
    private ModelUser user;

    private MyDrawerBuilder() {
        super(createSimpleMenuOption());   // <-- required because SimpleDrawerBuilder has no no‑arg constructor
        LightDarkButtonFooter lightDarkButtonFooter = (LightDarkButtonFooter) getFooter();
        lightDarkButtonFooter.addModeChangeListener(isDarkMode -> {
            // event for light dark mode changed
        });
    }
    public static MyDrawerBuilder getInstance() {
        if (instance == null) {
            instance = new MyDrawerBuilder();
        }
        return instance;
    }


    public ModelUser getUser() {
        return user;
    }

    public void setUser(ModelUser user) {
        boolean updateMenuItem = this.user == null || this.user.getRole() != user.getRole();
        this.user = user;

        MyMenuValidation.setUser(user);

        SimpleHeader header = (SimpleHeader) getHeader();
        SimpleHeaderData data = header.getSimpleHeaderData();

        AvatarIcon icon = null;

        // 1. Try custom image from user’s avatar path
        String customPath = user.getAvatarPath();
        if (customPath != null && !customPath.isEmpty()) {
            try {
                if (customPath.toLowerCase().endsWith(".svg")) {
                    FlatSVGIcon svgIcon = new FlatSVGIcon(customPath, 100, 100);
                    icon = new AvatarIcon(svgIcon, 50, 50, 3.5f);
                } else {
                    java.io.File file = new java.io.File(customPath);
                    if (file.exists()) {
                        icon = new AvatarIcon(file.toURI().toURL(), 50, 50, 3.5f);
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to load custom avatar: " + e.getMessage());
            }
        }

        // 2. Fallback to role‑based SVG
        if (icon == null) {
            String iconName = user.getRole() == ModelUser.Role.TEACHER ?
                    "avatar_male.svg" : "avatar_female.svg";
            FlatSVGIcon svgIcon = new FlatSVGIcon(
                    "raven/modal/demo/drawer/image/" + iconName, 100, 100);
            icon = new AvatarIcon(svgIcon, 50, 50, 3.5f);
        }

        // 3. Apply the same styling as the default header
        icon.setType(AvatarIcon.Type.MASK_SQUIRCLE);
        icon.setBorder(2, 2);
        changeAvatarIconBorderColor(icon);

        data.setIcon(icon);
        data.setTitle(user.getUserName());
        data.setDescription(user.getMail());
        header.setSimpleHeaderData(data);

        if (updateMenuItem) {
            rebuildMenu();
        }
    }

    @Override
    public SimpleHeaderData getSimpleHeaderData() {
        AvatarIcon icon = new AvatarIcon(new FlatSVGIcon("raven/modal/demo/drawer/image/avatar_male.svg", 100, 100), 50, 50, 3.5f);
        icon.setType(AvatarIcon.Type.MASK_SQUIRCLE);
        icon.setBorder(2, 2);

        changeAvatarIconBorderColor(icon);

        UIManager.addPropertyChangeListener(evt -> {
            if (evt.getPropertyName().equals("lookAndFeel")) {
                changeAvatarIconBorderColor(icon);
            }
        });

        return new SimpleHeaderData()
                .setIcon(icon)
                .setTitle("Ra Ven")
                .setDescription("raven@gmail.com");
    }

    private void changeAvatarIconBorderColor(AvatarIcon icon) {
        icon.setBorderColor(new AvatarIcon.BorderColor(UIManager.getColor("Component.accentColor"), 0.7f));
    }

    @Override
    public SimpleFooterData getSimpleFooterData() {
        return new SimpleFooterData()
                .setTitle("Swing Modal Dialog")
                .setDescription("Version " + Demo.DEMO_VERSION);
    }

    @Override
    public Option createOption() {
        Option option = super.createOption();
        option.setOpacity(0.3f);
        return option;
    }

    public static MenuOption createSimpleMenuOption() {
        // create simple menu option
        MenuOption simpleMenuOption = new MenuOption();

        MenuItem[] items = new MenuItem[]{
                new Item.Label("MAIN"),
                new Item("Dashboard", "dashboard.svg", FormDashboard.class),
                new Item.Label("SWING UI"),
                new Item("Forms", "forms.svg")
                        .subMenu("Table", FormTable.class),
                new Item.Label("OTHER"),
                new Item("Setting", "setting.svg", FormSetting.class),
                new Item("About", "about.svg"),
                new Item("Logout", "logout.svg")
        };

        simpleMenuOption.setMenuStyle(new MenuStyle() {

            @Override
            public void styleMenuItem(JButton menu, int[] index, boolean isMainItem) {
                boolean isTopLevel = index.length == 1;
                if (isTopLevel) {
                    // adjust item menu at the top level because it's contain icon
                    menu.putClientProperty(FlatClientProperties.STYLE, "" +
                            "margin:-1,0,-1,0;");
                }
            }

            @Override
            public void styleMenu(JComponent component) {
                component.putClientProperty(FlatClientProperties.STYLE, getDrawerBackgroundStyle());
            }
        });

        simpleMenuOption.getMenuStyle().setDrawerLineStyleRenderer(new DrawerStraightDotLineStyle());
        simpleMenuOption.setMenuValidation(new MyMenuValidation());

        simpleMenuOption.addMenuEvent((action, index) -> {
            System.out.println("Drawer menu selected " + Arrays.toString(index));
            Class<?> itemClass = action.getItem().getItemClass();
            int i = index[0];
            if (i == 3) {
                action.consume();
                FormManager.showAbout();
                return;
            } else if (i == 4) {
                action.consume();
                FormManager.logout();
                return;
            }
            if (itemClass == null || !Form.class.isAssignableFrom(itemClass)) {
                action.consume();
                return;
            }
            Class<? extends Form> formClass = (Class<? extends Form>) itemClass;
            FormManager.showForm(AllForms.getForm(formClass));
        });

        simpleMenuOption.setMenus(items)
                .setBaseIconPath("raven/modal/demo/drawer/icon")
                .setIconScale(0.45f);

        return simpleMenuOption;
    }

    @Override
    public int getOpenDrawerAt() {
        return 1000;
    }

    @Override
    public boolean openDrawerAtScale() {
        return false;
    }

    @Override
    public void build(DrawerPanel drawerPanel) {
        drawerPanel.putClientProperty(FlatClientProperties.STYLE, getDrawerBackgroundStyle() + "border:0,0,0,1,$Separator.foreground;");
    }

    private static String getDrawerBackgroundStyle() {
        return "background:$Menu.background;";
    }
}
