package uk.badamson.mc.ui;

import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;

import uk.badamson.mc.actor.message.SimpleRelativeLocation;

/**
 * <p>
 * Layout children in positions analogous to the their location (given by
 * {@linkplain Control#getLayoutData() layout data} that is a
 * {@link SimpleRelativeLocation}.
 * </p>
 * <p>
 * This layout assumes that the children are square.
 * </p>
 */
public final class SimpleRelativeLocationLayout extends Layout {

    public final int spacing = 4;

    @Override
    protected final Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
        Objects.requireNonNull(composite, "composite");

        final boolean defaultW = wHint == SWT.DEFAULT;
        final boolean defaultH = hHint == SWT.DEFAULT;
        final int wHintChild = defaultW ? SWT.DEFAULT : ((wHint - spacing * 6) / 7);
        final int hHintChild = defaultH ? SWT.DEFAULT : ((hHint - spacing * 6) / 7);
        int preferredChildWidth = 0;
        int preferredChildHeight = 0;
        for (Control child : composite.getChildren()) {
            final Point childPreferredSize = child.computeSize(wHintChild, hHintChild);
            preferredChildWidth = Integer.max(preferredChildWidth, childPreferredSize.x);
            preferredChildHeight = Integer.max(preferredChildHeight, childPreferredSize.y);
        }

        final int preferredChildSize = Integer.max(preferredChildWidth, preferredChildHeight);
        final int preferredSize = preferredChildSize * 7 + spacing * 6;
        return new Point(preferredSize, preferredSize);
    }

    @Override
    protected final void layout(Composite composite, boolean flushCache) {
        Objects.requireNonNull(composite, "composite");
        final Point size = composite.getSize();
        final int l = Integer.min(size.x, size.y);
        final int childSize = (l - 6 * spacing) / 7;
        final double r = childSize + spacing;
        final double origin = ((double) l - childSize) * 0.5;
        for (Control child : composite.getChildren()) {
            final Object layoutData = child.getLayoutData();
            Objects.requireNonNull(layoutData, "child layoutData");
            final SimpleRelativeLocation location = (SimpleRelativeLocation) layoutData;
            final double f;
            switch (location.getRange()) {
            case NEAR:
                f = 1;
                break;
            case MEDIUM:
                f = 2;
                break;
            case FAR:
                f = 3;
                break;
            default:// never happens
                f = 0.0;
                break;
            }
            final SimpleRelativeLocation.Direction direction = location.getDirection();
            final double x = origin + r * f * direction.getX();
            final double y = origin - r * f * direction.getY();
            child.setBounds((int) x, (int) y, childSize, childSize);
        }
    }

}
