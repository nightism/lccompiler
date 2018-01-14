#define DEBUG_TYPE "opCounter"


#include "llvm/ADT/SmallVector.h"
#include "llvm/IR/Function.h"
#include "llvm/Pass.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/Transforms/Utils/Local.h"
#include <map>
#include <vector>

using namespace llvm;
using namespace std;

namespace {
    struct CountOp : public FunctionPass {
        llvm::SmallVector<Instruction*, 128> WL;
        static char ID;

        CountOp() : FunctionPass(ID) {}
        virtual bool runOnFunction(Function &F) {
            findDeadCode(F);
            while (!WL.empty()) {
                eliminatedDeadCode();
                findDeadCode(F);
            }

            countInstruction(F);
            return false;
        }

        void findDeadCode(Function &F) {
            for (Function::iterator bb = F.begin(), e = F.end(); bb != e; ++bb) {
                for (BasicBlock::iterator i = bb->begin(), e = bb->end(); i != e; ++i) {
                    TargetLibraryInfo *TLI = NULL;
                    if (llvm::isInstructionTriviallyDead(&*i, NULL)) {
                        WL.push_back(&*i);
                    }
                }
            }
            return;
        }

        void eliminatedDeadCode() {
            while (!WL.empty()) {
                Instruction* I = WL.pop_back_val();
                I -> eraseFromParent();
            }
            return;
        }

        void countInstruction(Function &F) {
            std::map<std::string, int> opCounter;

            errs() << "Function " << F.getName() << '\n';
            for (Function::iterator bb = F.begin(), e = F.end(); bb != e; ++bb) {
                for (BasicBlock::iterator i = bb->begin(), e = bb->end(); i != e; ++i) {
                    if(opCounter.find(i->getOpcodeName()) == opCounter.end()) {
                        opCounter[i->getOpcodeName()] = 1;
                    } else {
                        opCounter[i->getOpcodeName()] += 1;
                    }
                }
            }

            std::map < std::string, int>::iterator i = opCounter.begin();
            std::map <std::string, int>::iterator e = opCounter.end();
            while (i != e) {
                errs() << i ->first << ": " << i ->second << "\n";
                i ++;
            }
            errs() << "\n";

            opCounter.clear();
            return ;
        }
    };
}

char CountOp::ID = 0;

__attribute__((unused)) static RegisterPass<CountOp>
    X("skeletonpass", "Counts opcodes per functions"); // NOLINT
