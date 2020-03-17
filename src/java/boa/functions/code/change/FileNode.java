package boa.functions.code.change;

import java.util.ArrayList;
import java.util.List;

import boa.functions.code.change.refactoring.RefactoringBonds;
import boa.types.Diff.ChangedFile;

public class FileNode {

	private ChangedFile cf = null;
	private RevNode r = null;
	private FileLocation loc = null;
	private TreeObjectId treeId = null;
	private List<FileLocation> prevLocs = new ArrayList<FileLocation>();
	private RefactoringBonds leftRefBonds = new RefactoringBonds();
	private RefactoringBonds rightRefBonds = new RefactoringBonds();
	
	// decl/method/field changes
	private List<ASTChange> astChanges = new ArrayList<ASTChange>();

	public FileNode(ChangedFile cf, RevNode r, FileLocation loc) {
		this.cf = cf;
		this.r = r;
		this.loc = loc;
	}
	
	public FileNode(ChangedFile cf, RevNode r) {
		this.cf = cf;
		this.r = r;
		this.loc = new FileLocation(cf.getRevisionIdx(), cf.getFileIdx());
	}

	public FileLocation getLoc() {
		return loc;
	}
	
	public int getRevIdx() {
		return cf.getRevisionIdx();
	}
	
	public int getFileIdx() {
		return cf.getFileIdx();
	}
	
	public TreeObjectId getTreeObjectId() {
		return treeId;
	}
	
	public void setTreeObjectId(TreeObjectId treeId) {
		this.treeId = treeId;
	}
	
	public ChangedFile getChangedFile() {
		return cf;
	}

	public RevNode getRev() {
		return r;
	}

	public TreeObjectId getListId() {
		return treeId;
	}

	public RefactoringBonds getLeftRefBonds() {
		return leftRefBonds;
	}

	public RefactoringBonds getRightRefBonds() {
		return rightRefBonds;
	}

	public List<FileLocation> getPrevLocs() {
		return prevLocs;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((loc == null) ? 0 : loc.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileNode other = (FileNode) obj;
		if (loc == null) {
			if (other.loc != null)
				return false;
		} else if (!loc.equals(other.loc))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return r.getRevision().getId() + " " + loc + " " + cf.getName();
	}

	public List<ASTChange> getAstChanges() {
		return astChanges;
	}

}