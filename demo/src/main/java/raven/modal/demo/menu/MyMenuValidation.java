package raven.modal.demo.menu;

import raven.modal.Drawer;
import raven.modal.demo.model.ModelUser;
import raven.modal.demo.system.Form;
import raven.modal.drawer.menu.MenuValidation;

public class MyMenuValidation extends MenuValidation {

    private static ModelUser user;

    public static void setUser(ModelUser user) {
        MyMenuValidation.user = user;
    }

    public static ModelUser getUser() {
        return user;
    }

    @Override
    public boolean menuValidation(int[] index) {
        return validation(index);
    }

    /**
     * Returns true if the menu at the given index should be visible,
     * false if it should be hidden.
     */
    public static boolean validation(int[] index) {
        if (user == null) {
            return false;                     // no user logged in → hide everything
        }
        if (user.getRole() == ModelUser.Role.TEACHER) {
            return true;                      // teacher sees all menus
        }

        // Student role – hide specific items (same as before for STAFF)
        boolean visible =
                checkMenu(index, new int[]{2, 0})      // `Modal` (Components → Modal)
                && checkMenu(index, new int[]{2, 1})    // `Toast` (Components → Toast)
                && checkMenu(index, new int[]{1, 2});   // `Responsive Layout` (Forms → Responsive Layout)

        return visible;
    }

    /**
     * Returns true if the two menu index arrays are not identical.
     * So checkMenu returns true if the item is NOT the one we want to hide.
     */
    private static boolean checkMenu(int[] index, int[] indexHide) {
        if (index.length == indexHide.length) {
            for (int i = 0; i < index.length; i++) {
                if (index[i] != indexHide[i]) {
                    return true;   // different item → visible
                }
            }
            return false;   // item matches the hidden one → invisible
        }
        return true;   // different depth → visible
    }

    /** Overload for validating a form class directly. */
    public static boolean validation(Class<? extends Form> itemClass) {
        int[] index = Drawer.getMenuIndexClass(itemClass);
        if (index == null) {
            return false;
        }
        return validation(index);
    }
}