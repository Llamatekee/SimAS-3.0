package utils;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Dialog;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public final class DialogUtils {

    private DialogUtils() {}

    /**
     * Prepare a Dialog (Alert, TextInputDialog, etc.) to be owned by the given owner and centered on it when shown.
     */
    public static void centerDialog(Dialog<?> dialog, Window owner) {
        if (dialog == null) return;
        if (owner != null) {
            try {
                dialog.initOwner(owner);
                dialog.initModality(Modality.WINDOW_MODAL);
            } catch (IllegalStateException ignored) {
                // initOwner/initModality must be called before showing; if already shown, ignore
            }
        }
        dialog.setOnShown(e -> {
            Window dialogWindow = dialog.getDialogPane().getScene() != null
                ? dialog.getDialogPane().getScene().getWindow()
                : null;
            if (dialogWindow != null && owner != null) {
                centerWindow(dialogWindow, owner);
            }
        });
    }

    /**
     * Prepare a non-modal Dialog to be owned by the given owner and centered on it.
     * Does NOT change modality, preserving non-blocking behavior for show().
     */
    public static void centerDialogNonModal(Dialog<?> dialog, Window owner) {
        if (dialog == null) return;
        if (owner != null) {
            try {
                dialog.initOwner(owner);
            } catch (IllegalStateException ignored) {
                // Already shown; ignore
            }
        }
        dialog.setOnShown(e -> {
            Window dialogWindow = dialog.getDialogPane().getScene() != null
                ? dialog.getDialogPane().getScene().getWindow()
                : null;
            if (dialogWindow != null && owner != null) {
                centerWindow(dialogWindow, owner);
            }
        });
    }

    /**
     * Overload that accepts any Node to infer its Window as owner.
     */
    public static void centerDialog(Dialog<?> dialog, Node ownerNode) {
        Window owner = ownerNode != null && ownerNode.getScene() != null ? ownerNode.getScene().getWindow() : null;
        centerDialog(dialog, owner);
    }

    /**
     * Overload for non-modal centering using a Node to infer owner.
     */
    public static void centerDialogNonModal(Dialog<?> dialog, Node ownerNode) {
        Window owner = ownerNode != null && ownerNode.getScene() != null ? ownerNode.getScene().getWindow() : null;
        centerDialogNonModal(dialog, owner);
    }

    /**
     * Center a child Stage relative to an owner Window once it's shown.
     */
    public static void centerStage(Stage child, Window owner) {
        if (child == null || owner == null) return;
        child.setOnShown(e -> centerWindow(child, owner));
    }

    /**
     * Overload that accepts any Node as the owner context.
     */
    public static void centerStage(Stage child, Node ownerNode) {
        Window owner = ownerNode != null && ownerNode.getScene() != null ? ownerNode.getScene().getWindow() : null;
        centerStage(child, owner);
    }

    /**
     * Core centering logic for any Window relative to an owner Window.
     */
    public static void centerWindow(Window dialogWindow, Window owner) {
        if (dialogWindow == null || owner == null) return;

        Runnable center = () -> {
            double x = owner.getX() + (owner.getWidth() - dialogWindow.getWidth()) / 2;
            double y = owner.getY() + (owner.getHeight() - dialogWindow.getHeight()) / 2;
            dialogWindow.setX(x);
            dialogWindow.setY(y);
        };

        if (dialogWindow.getWidth() == 0 || dialogWindow.getHeight() == 0) {
            Platform.runLater(center);
        } else {
            center.run();
        }
    }
}


