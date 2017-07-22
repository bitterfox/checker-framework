// Test for Issue 293:
// https://github.com/typetools/checker-framework/issues/293

class Issue293 {
    void test1() {
        String s;
        try {
            s = read();
        } catch (Exception e) {
            // Because of definite assignment, s cannot be mentioned here.
            write("Catch.");
            return;
        } finally {
            // Because of definite assignment, s cannot be mentioned here.
            write("Finally.");
        }

        // s is definitely initialized here.
        write(s);
    }

    void test2() {
        String s = "";
        try {
        } finally {
            write(s);
        }
    }

    String read() throws Exception {
        throw new Exception();
    }

    void write(String p) {
        System.out.println(p);
    }
}
