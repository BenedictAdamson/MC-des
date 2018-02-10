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
 */
public final class SimpleRelativeLocationLayout extends Layout {

    public final int horizontalSpacing = 4;
    public final int verticalSpacing = 4;

    @Override
    protected final Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
        Objects.requireNonNull(composite, "composite");

        final boolean defaultW = wHint == SWT.DEFAULT;
        final boolean defaultH = hHint == SWT.DEFAULT;
        final int wHintChild = defaultW ? SWT.DEFAULT : (wHint - verticalSpacing * 6) / 7;
        final int hHintChild = defaultH ? SWT.DEFAULT : (hHint - horizontalSpacing * 6) / 7;
        int preferredChildWidth = 0;
        int preferredChildHeight = 0;
        for (Control child : composite.getChildren()) {
            final Point childPreferredSize = child.computeSize(wHintChild, hHintChild);
            preferredChildWidth = Integer.max(preferredChildWidth, childPreferredSize.x);
            preferredChildHeight = Integer.max(preferredChildHeight, childPreferredSize.y);
        }
        return new Point(preferredChildWidth * 7 + horizontalSpacing * 6,
                preferredChildHeight * 7 + verticalSpacing * 6);
    }

    @Override
    protected final void layout(Composite composite, boolean flushCache) {
        Objects.requireNonNull(composite, "composite");
        final Point size = composite.getSize();
        final int childWidth = (size.x - 6 * horizontalSpacing) / 7;
        final int childHeight = (size.y - 6 * verticalSpacing) / 7;
        final double rx = (size.x - childWidth - horizontalSpacing) * 0.5;
        final double ry = (size.y - childHeight - verticalSpacing) * 0.5;
        for (Control child : composite.getChildren()) {
            final Object layoutData = child.getLayoutData();
            Objects.requireNonNull(layoutData, "child layoutData");
            final SimpleRelativeLocation location = (SimpleRelativeLocation) layoutData;
            final double f;
            switch (location.getRange()) {
            case NEAR:
                f = 1.0 / 3.0;
                break;
            case MEDIUM:
                f = 2.0 / 3.0;
                break;
            case FAR:
                f = 1.0;
                break;
            default:// never happens
                f = 0.0;
                break;
            }
            final SimpleRelativeLocation.Direction direction = location.getDirection();
            final double x = rx + rx * f * direction.getX();
            final double y = ry - ry * f * direction.getY();
            child.setBounds((int) x, (int) y, childWidth, childHeight);
        }
    }

}
