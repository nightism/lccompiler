// int func(int c, char d) {
//     int a;
//     char b;
//     return 1222;
// }

// int func1 (int a, int b, int c, int d, int e) {
//     return c;
// }

// int func2 (int a, int b, char c, int d, char e, char f, int g, char h, int i) {
//     return i;
// }

struct myst {
    int a;
    char b;
};

struct myst s1;


int a;
char b;

char c[20];
char d;

char * f;
int * g;


int main() {

    struct myst s2;

    int e[15];
    char * h;

    s1.a = 10;
    s2.a = 14;
    s1.b = 's';
    s2.b = read_c();
    print_s((char*)"\n");

    c[0] = '+';
    c[15] = '4';

    d = 'q';

    c[19] = '8';

    e[14] = 88;
    e[0] = 134;

    f = (char *) mcmalloc(sizeof(char));
    *f = 'f';
    g = (int *) mcmalloc(sizeof(int));
    h = (char *) mcmalloc(sizeof(char));

    *g = (int) 'a';
    *h = read_c();

    // int c;
    // int d;
    // char e;
    // int f;

    // a = read_i();
    // b = read_c();
    // c = 3;
    // d = 4;
    // e = 'e';
    // f = 6;

    // 1014s
    print_i(s1.a);
    print_i(s2.a);
    print_c(s1.b);
    print_c(s2.b);
    print_s((char*)"\n");

    // 88+q81344
    print_i(e[14]);
    print_c(c[0]);
    print_c(d);
    print_c(c[19]);
    print_i(e[0]);
    print_c(c[15]);
    print_s((char*)"\n");

    // f65
    print_c(*f);
    print_i(*g);
    print_c(*h);


}