package ru.totemguard.geometry;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * Represents a union of multiple CuboidRegions belonging to a single owner.
 * Internally stores individual cuboid components and can compute:
 * - Whether a position is inside any component
 * - The convex hull of all components (for boundary rendering)
 *
 * This class is mutable — components can be added and merged.
 */
public class UnionedRegion {
    private final List<CuboidRegion> components;

    public UnionedRegion() {
        this.components = new ArrayList<>();
    }

    public UnionedRegion(Collection<CuboidRegion> initial) {
        this.components = new ArrayList<>(initial);
    }

    /**
     * Adds a new cuboid to the region.
     * Does NOT automatically merge — call merge() after adding all components.
     */
    public void add(CuboidRegion region) {
        components.add(region);
    }

    /**
     * Removes a cuboid by its center position (approximate match).
     * Returns true if found and removed.
     */
    public boolean remove(BlockPos center) {
        for (Iterator<CuboidRegion> it = components.iterator(); it.hasNext(); ) {
            CuboidRegion comp = it.next();
            BlockPos compCenter = comp.getCenterXZ();
            if (compCenter.getX() == center.getX() && compCenter.getZ() == center.getZ()) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * Merges all intersecting cuboids in this region.
     * Uses an iterative approach: while any two cuboids intersect,
     * replace them with their union and continue.
     *
     * After merging, the component list is minimized.
     */
    public void merge() {
        if (components.size() < 2) {
            return; // Nothing to merge
        }

        boolean changed;
        do {
            changed = false;
            for (int i = 0; i < components.size(); i++) {
                for (int j = i + 1; j < components.size(); j++) {
                    CuboidRegion a = components.get(i);
                    CuboidRegion b = components.get(j);

                    if (a.intersects(b)) {
                        CuboidRegion merged = a.union(b);
                        components.set(i, merged);
                        components.remove(j);
                        changed = true;
                        break; // Restart outer loop
                    }
                }
                if (changed) break;
            }
        } while (changed && components.size() > 1);
    }

    /**
     * Checks if a block position is inside any component of this union.
     */
    public boolean contains(BlockPos pos) {
        for (CuboidRegion region : components) {
            if (region.contains(pos)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns an unmodifiable view of the component list.
     */
    public List<CuboidRegion> getComponents() {
        return Collections.unmodifiableList(components);
    }

    /**
     * Returns the number of cuboid components in this union.
     */
    public int getComponentCount() {
        return components.size();
    }

    /**
     * Computes the convex hull of all cuboid corners using Graham's Scan algorithm.
     * Returns a list of Vec3d representing the hull vertices in counter-clockwise order
     * on the XZ plane (Y is always 0).
     *
     * The hull is computed from the 4 corners of each cuboid component (projected to XZ).
     */
    public List<Vec3d> getConvexHull() {
        // Collect all corner points (XZ only, projected to world Y=0)
        Set<Vec3d> points = new HashSet<>();
        for (CuboidRegion comp : components) {
            points.add(new Vec3d(comp.getMinX(), 0, comp.getMinZ()));
            points.add(new Vec3d(comp.getMinX(), 0, comp.getMaxZ()));
            points.add(new Vec3d(comp.getMaxX(), 0, comp.getMinZ()));
            points.add(new Vec3d(comp.getMaxX(), 0, comp.getMaxZ()));
        }

        if (points.isEmpty()) {
            return Collections.emptyList();
        }

        if (points.size() <= 2) {
            return new ArrayList<>(points);
        }

        // Find the lowest-Y (then lowest-X) point as the pivot
        Vec3d pivot = null;
        for (Vec3d p : points) {
            if (pivot == null || p.getZ() < pivot.getZ() ||
                    (p.getZ() == pivot.getZ() && p.getX() < pivot.getX())) {
                pivot = p;
            }
        }

        final Vec3d finalPivot = pivot;

        // Sort points by polar angle relative to the pivot
        List<Vec3d> sorted = new ArrayList<>(points);
        sorted.sort((a, b) -> {
            if (a.equals(finalPivot)) return -1;
            if (b.equals(finalPivot)) return 1;

            double angleA = Math.atan2(a.getZ() - finalPivot.getZ(), a.getX() - finalPivot.getX());
            double angleB = Math.atan2(b.getZ() - finalPivot.getZ(), b.getX() - finalPivot.getX());

            int cmp = Double.compare(angleA, angleB);
            if (cmp != 0) return cmp;

            // If same angle, closer point first
            double distA = finalPivot.squaredDistanceTo(a);
            double distB = finalPivot.squaredDistanceTo(b);
            return Double.compare(distA, distB);
        });

        // Graham's Scan
        List<Vec3d> hull = new ArrayList<>();
        for (Vec3d point : sorted) {
            while (hull.size() >= 2 && crossProduct2D(
                    hull.get(hull.size() - 2),
                    hull.get(hull.size() - 1),
                    point) <= 0) {
                hull.remove(hull.size() - 1);
            }
            hull.add(point);
        }

        return hull;
    }

    /**
     * Computes the 2D cross product (on XZ plane) of vectors (o->a) and (o->b).
     * Positive = counter-clockwise turn, Negative = clockwise, Zero = collinear.
     */
    private static double crossProduct2D(Vec3d o, Vec3d a, Vec3d b) {
        double crossX = (a.getX() - o.getX()) * (b.getZ() - o.getZ());
        double crossZ = (a.getZ() - o.getZ()) * (b.getX() - o.getX());
        return crossX - crossZ;
    }

    /**
     * Checks if this region is empty (no components).
     */
    public boolean isEmpty() {
        return components.isEmpty();
    }

    /**
     * Returns a list of all totem center positions in this region.
     */
    public List<BlockPos> getTotemPositions() {
        List<BlockPos> positions = new ArrayList<>();
        for (CuboidRegion comp : components) {
            positions.add(comp.getCenterXZ());
        }
        return positions;
    }

    @Override
    public String toString() {
        return String.format("UnionedRegion[components=%d]", components.size());
    }
}
