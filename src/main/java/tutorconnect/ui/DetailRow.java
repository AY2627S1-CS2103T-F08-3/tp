package tutorconnect.ui;

import static java.util.Objects.requireNonNull;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * One line of the student profile. Its text stays the plain {@code "Key: value"} line (the panel's text contract),
 * while its skin draws the styled layout instead: a small key above the value, an optional section caption, or the
 * profile header with an initials avatar.
 */
public class DetailRow extends Label {
    private VBox content = new VBox();

    private DetailRow(String text) {
        super(requireNonNull(text));
        // Reports a width-dependent height to the panel, so wrapped values get the lines they need.
        setWrapText(true);
        setMaxWidth(Double.MAX_VALUE);
        getStyleClass().add("detail-row");
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new RowSkin(this);
    }

    private DetailRow show(VBox box) {
        content = box;
        content.getChildren().forEach(child -> {
            if (child instanceof Label label && !label.getStyleClass().contains("profile-meta")) {
                label.setMaxWidth(Double.MAX_VALUE);
            }
        });
        if (getSkin() instanceof RowSkin skin) {
            skin.setContent(content);
        }
        return this;
    }

    /** Lays the styled content out at the row's full width instead of drawing the row's own text. */
    private static class RowSkin extends SkinBase<DetailRow> {
        RowSkin(DetailRow row) {
            super(row);
            setContent(row.content);
        }

        void setContent(VBox box) {
            getChildren().setAll(box);
        }

        @Override
        protected double computePrefHeight(double width, double top, double right, double bottom, double left) {
            double contentWidth = width < 0 ? -1 : width - left - right;
            return top + getSkinnable().content.prefHeight(contentWidth) + bottom;
        }

        @Override
        protected double computeMinHeight(double width, double top, double right, double bottom, double left) {
            return computePrefHeight(width, top, right, bottom, left);
        }

        @Override
        protected double computePrefWidth(double height, double top, double right, double bottom, double left) {
            return left + getSkinnable().content.prefWidth(-1) + right;
        }

        @Override
        protected double computeMinWidth(double height, double top, double right, double bottom, double left) {
            return left + right;
        }

        @Override
        protected void layoutChildren(double x, double y, double width, double height) {
            getSkinnable().content.resizeRelocate(x, y, width, height);
        }
    }

    /** Creates the profile header for {@code name}, with {@code subtitle} under it (e.g. level and subject). */
    public static DetailRow header(String text, String name, String subtitle) {
        DetailRow row = new DetailRow(text);
        Label initials = new Label(Avatars.initials(name));
        initials.getStyleClass().addAll("avatar-text", "avatar-large-text");
        StackPane avatar = new StackPane(initials);
        avatar.getStyleClass().addAll("avatar", "avatar-large", Avatars.styleClass(name));
        Label title = new Label(name);
        title.getStyleClass().add("profile-name");
        title.setWrapText(true);
        title.setAlignment(Pos.CENTER);
        Label meta = new Label(subtitle);
        meta.getStyleClass().add("profile-meta");
        meta.setWrapText(true);
        VBox box = new VBox(10, avatar, title, meta);
        box.getStyleClass().add("profile-header");
        return row.show(box);
    }

    /**
     * Creates a key/value row from a {@code "Key: value"} line. When {@code section} is not {@code null}, a section
     * caption is drawn above the row.
     */
    public static DetailRow field(String text, String section) {
        DetailRow row = new DetailRow(text);
        int separator = text.indexOf(": ");
        String key = separator < 0 ? text : text.substring(0, separator);
        String value = separator < 0 ? "" : text.substring(separator + 2);
        Label keyLabel = new Label(key);
        keyLabel.getStyleClass().add("detail-key");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add(value.equals("—") ? "detail-value-unset" : "detail-value");
        valueLabel.setWrapText(true);
        VBox box = new VBox(2, keyLabel, valueLabel);
        if (section != null) {
            Label caption = new Label(section);
            caption.getStyleClass().add("detail-section");
            box.getChildren().add(0, caption);
        }
        return row.show(box);
    }

    /**
     * Creates a caption line such as {@code "Assessments (2):"}, shown without its trailing colon, with an optional
     * section caption above it.
     */
    public static DetailRow caption(String text, String section) {
        DetailRow row = new DetailRow(text);
        Label caption = new Label(text.endsWith(":") ? text.substring(0, text.length() - 1) : text);
        caption.getStyleClass().add("detail-key");
        VBox box = new VBox(2, caption);
        if (section != null) {
            Label sectionLabel = new Label(section);
            sectionLabel.getStyleClass().add("detail-section");
            box.getChildren().add(0, sectionLabel);
        }
        return row.show(box);
    }

    /** Creates the prompt shown when no student is selected, with a keyboard hint under it. */
    public static DetailRow emptyState(String text, String hint) {
        DetailRow row = new DetailRow(text);
        Label title = new Label(text);
        title.getStyleClass().add("empty-title");
        title.setWrapText(true);
        Label tip = new Label(hint);
        tip.getStyleClass().add("empty-hint");
        tip.setWrapText(true);
        VBox box = new VBox(6, title, tip);
        box.getStyleClass().add("empty-state");
        return row.show(box);
    }

    /** Creates an indented list item such as one assessment record or one payment month. */
    public static DetailRow item(String text) {
        DetailRow row = new DetailRow(text);
        Label item = new Label(text);
        item.getStyleClass().add("detail-item");
        item.setWrapText(true);
        return row.show(new VBox(item));
    }
}
