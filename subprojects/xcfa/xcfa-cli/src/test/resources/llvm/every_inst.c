int ifoo(int a, int b) {
    b = a;
    return -1;
}

void foo() {
}

#include <stdio.h>

int main() {
    int a;
    scanf("%d", &a);
    if(a) {
        printf("if\n");
    }
    else {
        printf("else\n");
    }

    switch(a) {
        case 0: printf("0\n"); break;
        case 1: printf("1\n"); break;
        case 2: printf("2\n"); break;
        default: printf("other\n"); break;
    }

    int b = a + 2;
    int c = b - 3;
    int d = c*c;
    int e = d/c;
    int f = d % 2;

    int i = a || b;

    foo();
    return ifoo(a, b);
}
