package info.boytsov.lucene;

/*************************************************************************
 *  
 *  Count number of inversions in N log N time.
 *  
 *  Copyright © 2002–2010, Robert Sedgewick and Kevin Wayn
 *  
 *  Taken from http://algs4.cs.princeton.edu/22mergesort/Inversions.java.html
 *
 *************************************************************************/

public class Inversions {

    // merge and count
    @SuppressWarnings("rawtypes")
    private static long merge(Comparable[] a, Comparable[] aux, int lo, int mid, int hi) {
        long inversions = 0;

        // copy to aux[]
        for (int k = lo; k <= hi; k++) {
            aux[k] = a[k]; 
        }

        // merge back to a[]
        int i = lo, j = mid+1;
        for (int k = lo; k <= hi; k++) {
            if      (i > mid)                a[k] = aux[j++];
            else if (j > hi)                 a[k] = aux[i++];
            else if (less(aux[j], aux[i])) { a[k] = aux[j++]; inversions += (mid - i + 1); }
            else                             a[k] = aux[i++];
        }
        return inversions;
    }

    // return the number of inversions in the subarray b[lo..hi]
    // side effect b[lo..hi] is rearranged in ascending order
    @SuppressWarnings("rawtypes")
    private static long count(Comparable[] a, 
                              Comparable[] b, 
                              Comparable[] aux, int lo, int hi) {
        long inversions = 0;
        if (hi <= lo) return 0;
        int mid = lo + (hi - lo) / 2;
        inversions += count(a, b, aux, lo, mid);  
        inversions += count(a, b, aux, mid+1, hi);
        inversions += merge(b, aux, lo, mid, hi);
        assert inversions == brute(a, lo, hi);
        return inversions;
    }


    // count number of inversions in the array a[] - do not overwrite a[]
    @SuppressWarnings("rawtypes")
    public static long count(Comparable[] a) {
        Comparable[] b   = new Comparable[a.length];
        Comparable[] aux = new Comparable[a.length];
        for (int i = 0; i < a.length; i++) b[i] = a[i];
        long inversions = count(a, b, aux, 0, a.length - 1);
        return inversions;
    }


    // is v < w ?
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static boolean less(Comparable v, Comparable w) {
        return (v.compareTo(w) < 0);
    }

    // count number of inversions in a[lo..hi] via brute force (for debugging only)
    private static long brute(@SuppressWarnings("rawtypes") Comparable[] a, int lo, int hi) {
        long inversions = 0;
        for (int i = lo; i <= hi; i++)
            for (int j = i + 1; j <= hi; j++)
                if (less(a[j], a[i])) inversions++;
        return inversions;
    }


    // count number of inversions via brute force
    public static long brute(@SuppressWarnings("rawtypes") Comparable[] a) {
        return brute(a, 0, a.length - 1);
    }
}
