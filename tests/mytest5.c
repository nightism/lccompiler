struct foo {
int x;
int y;
char name[10];
};

struct fooStruct {
int x;
struct foo y;
};

struct Node {
int val;
struct Node next;
};

int main() {
struct foo a;
struct foo* y;
struct foo lst[2];
int result;
a.x = 10;
result = (*y).x;
result = (a).x;
result = 5 + -a.x * 2;
result = lst[0].x;
result = a.name[0];
result = (*y).name[0];
}
