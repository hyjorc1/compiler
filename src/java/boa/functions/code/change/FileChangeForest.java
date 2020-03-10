package boa.functions.code.change;

import static boa.functions.BoaAstIntrinsics.getCodeChange;
import static boa.functions.BoaIntrinsics.getRevision;
import static boa.functions.code.change.refactoring.BoaRefactoringPredictionIntrinsics.getRefactorings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import boa.functions.code.change.refactoring.BoaRefactoringPredictionIntrinsics;
import boa.types.Code.CodeRefactoring;
import boa.types.Code.CodeRepository;
import boa.types.Code.Revision;
import boa.types.Diff.ChangedFile;
import boa.types.Shared.ChangeKind;
import boa.types.Toplevel.Project;

public class FileChangeForest {

	// tree properties
	protected List<FileTree> trees = new ArrayList<FileTree>();
	protected HashMap<String, HashSet<String>> fileObjectIdToLocs = new HashMap<String, HashSet<String>>();
	protected HashMap<String, FileNode> fileLocIdToNode = new HashMap<String, FileNode>();
	
	protected boolean debug = false;
	
	// revision info
	protected HashSet<String> nContributor = new HashSet<String>();
	protected HashMap<Integer, RevNode> revIdxMap = new HashMap<Integer, RevNode>();
	protected HashMap<String, RevNode> revIdMap = new HashMap<String, RevNode>();
	

	public FileChangeForest(CodeRepository cr, int revCount, boolean debug) {
		for (int i = 0; i < revCount; i++)
			getRev(cr, i);
		this.debug = debug;
		updateLists();
	}

	private void updateLists() {
		for (int i = revIdxMap.size() - 1; i >= 0; i--) {
			RevNode r = revIdxMap.get(i);
			for (FileNode fn : r.getJavaFileNodes()) {
				if (!fileLocIdToNode.containsKey(fn.getLocId())) {
					FileTree list = new FileTree(this, fn, trees.size());
					if (list.linkAll())
						trees.add(list);
				}
			}
		}
	}
	
	private RevNode getRev(CodeRepository cr, int idx) {
		if (revIdxMap.containsKey(idx))
			return revIdxMap.get(idx);
		Revision r = getRevision(cr, idx);
		nContributor.add(r.getAuthor().getUsername());
		RevNode rev = new RevNode(idx, r, nContributor.size());
		revIdxMap.put(idx, rev);
		revIdMap.put(r.getId(), rev);
		return revIdxMap.get(idx);
	}

	public List<FileTree> getTreesAsList() {
		return this.trees;
	}
	
	// refactoring info

	
	public void updateRefLists(Project p, HashSet<String> refRevIds, Set<String> refTypes) {
		for (String id : refRevIds) {
			RevNode r = revIdMap.get(id);
			List<CodeRefactoring> refs = refTypes == null ? getCodeChange(p, r.getRevision()).getRefactoringsList()
					: getRefactorings(p, r.getRevision(), refTypes);
			for (CodeRefactoring ref : refs) {
//				System.out.println(r.getRevision().getId());
//				System.out.println(ref.getDescription());
				String beforeFilePath = ref.getLeftSideLocations(0).getFilePath();
//				System.out.println(beforeFilePath);
				FileNode fileBefore = findLastModification(beforeFilePath, r, ref);
				String afterFilePath = ref.getRightSideLocations(0).getFilePath();
//				System.out.println(afterFilePath);
				FileNode fileAfter = getFileNodeFrom(afterFilePath, r);

				
				if (!fileLocIdToNode.containsKey(fileBefore.getLocId()))
					System.err.println("err 1");
				int treeIdx = fileLocIdToNode.get(fileBefore.getLocId()).getTreeObjectId().getAsInt();
				FileTree tree = trees.get(treeIdx);
				if (!fileLocIdToNode.get(fileBefore.getLocId()).equals(fileBefore)) {
					System.err.println(ref.getDescription());
				}
				tree.refLocs.add(fileBefore.getLocId());
			}
		}
	}
	
	private FileNode getFileNodeFrom(String filePath, RevNode r) {
		for (ChangedFile cf : r.getRevision().getFilesList())
			if (cf.getName().equals(filePath))
				return fileLocIdToNode.get(cf.getRevisionIdx() + " " + cf.getFileIdx());
		return null;
	}
	
	private FileNode findLastModification(String fileName, RevNode r, CodeRefactoring ref) {
		FileNode fn = getFileNodeFrom(fileName, r);
		if (fn != null && fn.getChangedFile().getChange() == ChangeKind.ADDED)
			return fn;
		RevNode cur = r;
		do {
			if (cur.getRevision().getParentsCount() == 0)
				return null;
			// first parent in main branch
			cur = revIdxMap.get(cur.getRevision().getParents(0));
			fn = getFileNodeFrom(fileName, cur);
			if (fn != null)
				return fn;
			
		} while (true);
	}
}
