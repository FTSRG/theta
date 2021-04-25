volatile int a;
int adder(int a, int b) {
    int c = a; int d = b;
    return adder(c, d);
}
int main() {
    volatile int b;
    return adder(a,b);
}